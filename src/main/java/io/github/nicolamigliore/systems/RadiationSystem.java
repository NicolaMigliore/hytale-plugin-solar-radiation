package io.github.nicolamigliore.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.radiation.*;
import javax.annotation.Nonnull;

public class RadiationSystem extends EntityTickingSystem<EntityStore> {
    private final RadiationSettings settings;

    public RadiationSystem(RadiationSettings settings) { this.settings = settings; }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(RadiationComponent.getComponentType(), Player.getComponentType(),
                PlayerRef.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        if (!Float.isFinite(dt) || dt <= 0) return;
        var ref = chunk.getReferenceTo(index);
        var radiation = chunk.getComponent(index, RadiationComponent.getComponentType());
        var player = chunk.getComponent(index, Player.getComponentType());
        var world = store.getExternalData().getWorld();
        var effects = buffer.ensureAndGetComponent(ref, EffectControllerComponent.getComponentType());
        boolean dead = buffer.getComponent(ref, DeathComponent.getComponentType()) != null;
        boolean exempt = player.getGameMode() != GameMode.Adventure;
        boolean enabled = settings.isEnabled(world.getName());
        if (dead || exempt || !enabled) {
            if (dead) radiation.setTotalRadiations(0);
            radiation.invalidateExposure();
            radiation.setExposure(dead ? Exposure.DEAD : exempt ? Exposure.EXEMPT : Exposure.DISABLED);
            RadiationEffects.update(ref, effects, 0, buffer);
            return;
        }

        if (radiation.shouldCheckExposure(dt)) {
            var time = store.getResource(WorldTimeResource.getResourceType());
            // Hytale's scaled solar phase maps sunrise/sunset to quarter/three-quarter day.
            if (!time.isScaledDayTimeWithinRange(0.25, 0.75)) {
                radiation.setExposure(Exposure.NIGHT);
            } else {
                var position = chunk.getComponent(index, TransformComponent.getComponentType()).getPosition();
                var bounds = buffer.getComponent(ref, BoundingBox.getComponentType());
                double headHeight = bounds == null ? 1.8 : bounds.getBoundingBox().max.y;
                radiation.setExposure(SolarExposure.sample(world,
                        (int) Math.floor(position.x), (int) Math.ceil(position.y + headHeight),
                        (int) Math.floor(position.z), settings));
            }
        }
        float previousDose = radiation.getTotalRadiations();
        radiation.setTotalRadiations(radiation.getExposure().advance(previousDose, dt));
        int stage = radiation.getRadLevel();
        RadiationEffects.update(ref, effects, stage, buffer);

        var recipient = chunk.getComponent(index, PlayerRef.getComponentType());
        if (previousDose < 5 && radiation.getTotalRadiations() >= 5) {
            recipient.sendMessage(Message.raw("Radiation detected. Seek shelter."));
        }
        if (stage != radiation.getLastReportedStage()) {
            if (stage > 0 || radiation.getLastReportedStage() > 0) {
                recipient.sendMessage(Message.raw("Radiation stage %d | dose %.2f / 180".formatted(
                        stage, radiation.getTotalRadiations())));
            }
            radiation.setLastReportedStage(stage);
        }
    }
}
