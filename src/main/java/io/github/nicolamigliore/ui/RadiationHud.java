package io.github.nicolamigliore.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.radiation.Exposure;
import javax.annotation.Nullable;

/** Owned by the player's HUD manager, with no global player map or background timer. */
public class RadiationHud extends CustomUIHud {
    public static final String KEY = "SolarRadiation:Status";
    public static final String LAYOUT = "Hud/SolarRadiation.ui";
    private static final float UPDATE_INTERVAL = 0.25f;
    private static final float EFFECT_DURATION = 0.75f;

    private RadiationHudState lastSent;
    private float remaining = UPDATE_INTERVAL;
    private float elapsedSinceRefresh;
    private float effectFrom;
    private float effectTarget;
    private float effectValue;
    private float effectRemaining;
    private int lastEffectPixels;
    private boolean lastEffectVisible;

    public RadiationHud(PlayerRef playerRef, RadiationHudState initial) {
        super(playerRef, KEY);
        lastSent = initial;
        effectValue = effectTarget = initial.barValue();
        lastEffectPixels = initial.fillPixels();
    }

    @Override
    protected void build(UICommandBuilder builder) {
        builder.append(LAYOUT);
        writeChanges(builder, null, lastSent);
        builder.set("#RadiationBarEffect.Value", lastSent.barValue());
        builder.set("#RadiationBarEffect.Visible", false);
    }

    public static void sync(HudManager manager, PlayerRef playerRef, boolean ready, float dt,
                            @Nullable RadiationComponent dose, Exposure exposure) {
        if (!ready || !Float.isFinite(dt) || dt <= 0) return;
        if (dose == null) {
            remove(manager, playerRef);
            return;
        }
        var current = manager.getCustomHud(KEY);
        if (current instanceof RadiationHud hud && hud.getPlayerRef() == playerRef) {
            hud.refresh(dt, dose.getTotalRadiations(), exposure);
        } else {
            manager.addCustomHud(playerRef, new RadiationHud(playerRef,
                    RadiationHudState.of(dose.getTotalRadiations(), exposure)));
        }
    }

    public static void remove(HudManager manager, PlayerRef playerRef) {
        if (manager.getCustomHud(KEY) instanceof RadiationHud) manager.removeCustomHud(playerRef, KEY);
    }

    private void refresh(float dt, float dose, Exposure exposure) {
        elapsedSinceRefresh += dt;
        remaining -= dt;
        if (remaining > 0) return;
        remaining = UPDATE_INTERVAL;
        var next = RadiationHudState.of(dose, exposure);
        float elapsed = elapsedSinceRefresh;
        elapsedSinceRefresh = 0;
        if (next.equals(lastSent) && effectRemaining <= 0 && !lastEffectVisible) return;
        var builder = new UICommandBuilder();
        updateEffect(builder, next, elapsed);
        writeChanges(builder, lastSent, next);
        if (builder.getCommands().length > 0) update(false, builder);
        lastSent = next;
    }

    private void updateEffect(UICommandBuilder builder, RadiationHudState next, float elapsed) {
        if (effectRemaining > 0) {
            effectRemaining = Math.max(0, effectRemaining - elapsed);
            effectValue = effectTarget + (effectFrom - effectTarget) * (effectRemaining / EFFECT_DURATION);
        }
        boolean active = switch (next.exposure()) {
            case EXPOSED, SHELTERED, NIGHT, RECOVERING -> true;
            default -> false;
        };
        if (!active) {
            effectRemaining = 0;
            effectValue = effectTarget = next.barValue();
        } else if (lastSent.fillPixels() != next.fillPixels()) {
            // Move toward the old target before retargeting, so continued recovery does not freeze the trail.
            effectFrom = Math.max(effectValue, Math.max(lastSent.barValue(), next.barValue()));
            effectValue = effectFrom;
            effectTarget = next.barValue();
            effectRemaining = EFFECT_DURATION;
        }
        int pixels = Math.round(effectValue * RadiationHudState.BAR_WIDTH);
        if (pixels != lastEffectPixels) {
            builder.set("#RadiationBarEffect.Value", pixels / (float) RadiationHudState.BAR_WIDTH);
            lastEffectPixels = pixels;
        }
        boolean visible = active && effectRemaining > 0 && pixels > 0;
        if (visible != lastEffectVisible) {
            builder.set("#RadiationBarEffect.Visible", visible);
            lastEffectVisible = visible;
        }
    }

    private static void writeChanges(UICommandBuilder builder, @Nullable RadiationHudState previous,
                                      RadiationHudState next) {
        if (previous == null || previous.fillPixels() != next.fillPixels()) {
            builder.set("#RadiationFill.Value", next.barValue());
        }
        if (previous == null || previous.stage() != next.stage()) {
            builder.set("#RadiationStage.Text", next.stageText());
        }
        if (previous == null || previous.exposure() != next.exposure()) {
            builder.set("#RadiationExposure.Text", next.exposureText());
        }
    }
}
