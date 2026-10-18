package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import io.github.nicolamigliore.components.RadiationComponent;
import java.util.List;
import org.joml.Vector3d;

/** Only invoked at completed native consumption or projectile impact, never during prediction. */
public final class DecontaminationInteraction extends SimpleInstantInteraction {
    private final RadiationSettings settings;
    private final ComponentType<EntityStore, SpentFlask> spentType;
    private boolean splash;

    private DecontaminationInteraction(RadiationSettings settings, ComponentType<EntityStore, SpentFlask> spentType) {
        this.settings = settings;
        this.spentType = spentType;
    }

    public static BuilderCodec<DecontaminationInteraction> codec(RadiationSettings settings,
            ComponentType<EntityStore, SpentFlask> spentType) {
        return BuilderCodec.builder(DecontaminationInteraction.class,
                        () -> new DecontaminationInteraction(settings, spentType), SimpleInstantInteraction.CODEC)
                .append(new KeyedCodec<>("Splash", Codec.BOOLEAN), (s, v) -> s.splash = v, s -> s.splash).add()
                .build();
    }

    @Override
    public WaitForDataFrom getWaitForDataFrom() { return WaitForDataFrom.Server; }

    public static void validateAssets() {
        var drink = Item.getAssetMap().getAsset("SolarRadiation_Treatment");
        var flask = Item.getAssetMap().getAsset("SolarRadiation_Treatment_Splash");
        var projectile = ProjectileConfig.getAssetMap().getAsset("SolarRadiation_Treatment_Projectile");
        var impact = RootInteraction.getAssetMap().getAsset("SolarRadiation_Treatment_Impact");
        if (drink == null || flask == null || projectile == null || impact == null) {
            throw new IllegalStateException("Missing decontamination item, projectile or impact assets");
        }
        var effect = RootInteraction.getAssetMap().getAsset(drink.getInteractionVars().get("Effect"));
        if (effect == null || effect.getInteractionIds().length != 1
                || !(Interaction.getAssetMap().getAsset(effect.getInteractionIds()[0])
                    instanceof DecontaminationInteraction treatment) || treatment.splash) {
            throw new IllegalStateException("Treatment drink must replace the native antidote effect with decontamination");
        }
        for (var type : new InteractionType[]{InteractionType.ProjectileHit, InteractionType.ProjectileMiss}) {
            if (!impact.getId().equals(projectile.getInteractions().get(type))) {
                throw new IllegalStateException("Treatment flask must handle both entity and terrain impact");
            }
        }
    }

    @Override
    protected void firstRun(InteractionType type, InteractionContext context, CooldownHandler cooldown) {
        var buffer = context.getCommandBuffer();
        var source = context.getEntity();
        if (buffer == null || source == null || !source.isValid()) return;
        if (!splash) {
            treat(source, buffer);
            return;
        }
        var spent = buffer.ensureAndGetComponent(source, spentType);
        if (spent.used) return;
        spent.used = true;
        var transform = buffer.getComponent(source, TransformComponent.getComponentType());
        if (transform == null) return;
        var hit = context.getMetaStore().getIfPresentMetaObject(Interaction.HIT_LOCATION);
        var origin = hit == null ? new Vector3d(transform.getPosition()) : new Vector3d(hit.x, hit.y, hit.z);
        var incoming = new Vector3d(transform.getPosition()).sub(origin);
        var physics = buffer.getComponent(source, StandardPhysicsProvider.getComponentType());
        // Native terrain callbacks report the projectile body's position. Use the actual
        // contact surface for splash range and start just outside it for visibility.
        if (type == InteractionType.ProjectileMiss && physics != null
                && physics.getContactNormal().lengthSquared() > 1e-12) {
            origin.set(physics.getContactPosition());
            incoming.set(physics.getContactNormal());
        }
        var rayOrigin = TreatmentGeometry.surfaceOrigin(origin, incoming);
        var chunks = buffer.getExternalData().getWorld().getChunkStore();
        TreatmentGeometry.OpenVoxel open = (x, y, z) -> {
            var sectionRef = chunks.getChunkSectionReferenceAtBlock(x, y, z);
            if (sectionRef == null || !sectionRef.isValid()) return false;
            var section = chunks.getStore().getComponent(sectionRef, BlockSection.getComponentType());
            if (section == null) return false;
            int id = section.get(x & 31, y & 31, z & 31);
            if (id == 0) return true;
            var block = BlockType.getAssetMap().getAsset(id);
            return block != null && block.getMaterial() != BlockMaterial.Solid;
        };
        double radius = settings.getTreatmentRadius();
        // This overload collects nearby players itself; the explicit-recipient overload
        // requires a non-null list. A world-space burst survives removal of the flask.
        ParticleUtil.spawnParticleEffect("SolarRadiation_Treatment_Splash", rayOrigin,
                0f, 0f, 0f, (float) radius / 3f, 1f, buffer);
        // Copy the native spatial query's reusable list before modifying effect controllers.
        for (var target : List.copyOf(TargetUtil.getAllEntitiesInSphere(origin, radius, buffer))) {
            if (!target.isValid()) continue;
            var position = buffer.getComponent(target, TransformComponent.getComponentType());
            var bounds = buffer.getComponent(target, BoundingBox.getComponentType());
            if (position != null && TreatmentGeometry.canTreat(origin, rayOrigin, position.getPosition(),
                    bounds == null ? null : bounds.getBoundingBox(), radius, open)) {
                treat(target, buffer);
            }
        }
    }

    static boolean clearDose(RadiationComponent dose, Runnable clearSymptoms) {
        if (dose == null) return false;
        dose.setTotalRadiations(0);
        clearSymptoms.run();
        return true;
    }

    private static void treat(Ref<EntityStore> target, ComponentAccessor<EntityStore> accessor) {
        if (!target.isValid() || accessor.getComponent(target, DeathComponent.getComponentType()) != null) return;
        if (accessor.getComponent(target, Player.getComponentType()) == null
                && accessor.getComponent(target, NPCEntity.getComponentType()) == null) return;
        clearDose(accessor.getComponent(target, RadiationComponent.getComponentType()), () -> {
            var effects = accessor.getComponent(target, EffectControllerComponent.getComponentType());
            if (effects != null) RadiationEffects.clear(target, effects, accessor);
        });
    }

    /** Runtime-only guard against repeated impact callbacks on the same flask. */
    public static final class SpentFlask implements Component<EntityStore> {
        private boolean used;
        @Override public SpentFlask clone() { var copy = new SpentFlask(); copy.used = used; return copy; }
        @Override public Component<EntityStore> cloneSerializable() { return null; }
    }
}
