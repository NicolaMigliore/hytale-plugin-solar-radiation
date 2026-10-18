package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.livingentity.LivingEntityEffectSystem;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.components.RadiationRuntimeComponent;
import io.github.nicolamigliore.radiation.*;
import javax.annotation.Nonnull;
import java.util.Set;

/** Shared dose/effect processing for opted-in entities; sunlight and chat are player-only. */
public class RadiationSystem extends EntityTickingSystem<EntityStore> {
    private final RadiationSettings settings;

    public RadiationSystem(RadiationSettings settings) { this.settings = settings; }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        // Resolve recovery and tier changes before native effects can deal damage this tick.
        return Set.of(new SystemDependency<>(Order.BEFORE, LivingEntityEffectSystem.class));
    }

    @Override
    public Query<EntityStore> getQuery() {
        return RadiationComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        if (!Float.isFinite(dt) || dt <= 0) return;
        var ref = chunk.getReferenceTo(index);
        var radiation = chunk.getComponent(index, RadiationComponent.getComponentType());
        var runtime = buffer.ensureAndGetComponent(ref, RadiationRuntimeComponent.getComponentType());
        var player = buffer.getComponent(ref, Player.getComponentType());
        var recipient = buffer.getComponent(ref, PlayerRef.getComponentType());
        var subject = player != null || recipient != null ? RadiationSubject.PLAYER : RadiationSubject.MOB;
        var world = store.getExternalData().getWorld();
        var effects = buffer.ensureAndGetComponent(ref, EffectControllerComponent.getComponentType());
        boolean dead = buffer.getComponent(ref, DeathComponent.getComponentType()) != null;
        boolean exempt = player != null && player.getGameMode() != GameMode.Adventure;
        boolean enabled = settings.isEnabled(world.getName());
        if (dead || exempt || !enabled) {
            if (dead) radiation.setTotalRadiations(0);
            runtime.invalidateExposure();
            runtime.setExposure(dead ? Exposure.DEAD : exempt ? Exposure.EXEMPT : Exposure.DISABLED);
            RadiationEffects.update(ref, effects, RadiationSymptoms.NONE, buffer);
            return;
        }

        var transform = buffer.getComponent(ref, TransformComponent.getComponentType());
        if (subject == RadiationSubject.PLAYER && (player == null || recipient == null || transform == null)) {
            // A player can be partially initialized during join or transfer; never treat it as a mob.
            runtime.invalidateExposure();
            RadiationEffects.update(ref, effects, RadiationSymptoms.NONE, buffer);
            return;
        }
        if (subject == RadiationSubject.MOB) {
            // Mob dose is opt-in. A later contamination system supplies it; sunlight never does.
            runtime.setExposure(Exposure.RECOVERING);
        } else {
            var time = store.getResource(WorldTimeResource.getResourceType());
            if (!Daylight.isDay(time)) {
                // Night is known safe without terrain reads. Reset the timer for the next sunrise.
                runtime.invalidateExposure();
                runtime.setExposure(Exposure.NIGHT);
            } else if (runtime.shouldCheckExposure(dt, settings.getExposureCheckInterval())) {
                var position = transform.getPosition();
                var bounds = buffer.getComponent(ref, BoundingBox.getComponentType());
                double headHeight = bounds == null ? 1.8 : bounds.getBoundingBox().max.y;
                runtime.setExposure(SolarExposure.sample(world,
                        (int) Math.floor(position.x), (int) Math.ceil(position.y + headHeight),
                        (int) Math.floor(position.z), settings));
            }
        }
        float previousDose = radiation.getTotalRadiations();
        radiation.setTotalRadiations(subject.advance(previousDose, runtime.getExposure(), dt, settings));
        RadiationEffects.update(ref, effects, subject.symptoms(radiation.getTotalRadiations()), buffer);

        if (recipient == null) return;
        if (previousDose < 5 && radiation.getTotalRadiations() >= 5) {
            recipient.sendMessage(Message.raw("Radiation detected. Seek shelter."));
        }
    }
}
