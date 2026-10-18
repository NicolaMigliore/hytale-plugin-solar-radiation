package io.github.nicolamigliore.ui;

import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.ToServerPacket;
import com.hypixel.hytale.protocol.packets.interface_.CustomHud;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import io.github.nicolamigliore.components.RadiationComponent;
import io.github.nicolamigliore.radiation.Exposure;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Uses the native HUD manager, builder, and packets, recording writes instead of opening a socket. */
class RadiationHudTest {
    private final RecordingPackets packets = new RecordingPackets();
    private final PlayerRef playerRef = player(packets);
    private final HudManager manager = new HudManager();
    private final RadiationComponent dose = new RadiationComponent(20.5f);

    @Test
    void waitsForClientReadinessAndSendsOneInitialLayoutWithCurrentValues() {
        sync(false, 1, Exposure.UNKNOWN);
        assertTrue(packets.sent.isEmpty());
        sync(true, 0.05f, Exposure.SHELTERED);
        assertEquals(1, packets.sent.size());
        var initial = packets.last();
        assertTrue(initial.clear);
        assertEquals(RadiationHud.KEY, initial.hudId);
        assertEquals(CustomUICommandType.Append, initial.commands[0].type);
        assertEquals(RadiationHud.LAYOUT, initial.commands[0].text);
        assertFalse(selectors(initial).contains("#RadiationDose.Text"));
        assertTrue(value(initial, "#RadiationBarEffect.Visible").contains("false"));
        assertTrue(value(initial, "#RadiationStage.Text").contains("Stage 1"));
        assertTrue(value(initial, "#RadiationExposure.Text").contains("Sheltered"));
        for (int i = 0; i < 100; i++) sync(true, 0.05f, Exposure.SHELTERED);
        assertEquals(1, packets.sent.size(), "A stable HUD must not resend its layout or labels");
    }

    @Test
    void changesAreThrottledAndOnlyChangedLabelsArePatched() {
        sync(true, 0.05f, Exposure.EXPOSED);
        packets.sent.clear();
        dose.setTotalRadiations(21.5f);
        sync(true, 0.125f, Exposure.EXPOSED);
        assertTrue(packets.sent.isEmpty());
        sync(true, 0.125f, Exposure.EXPOSED);
        var patch = packets.last();
        assertFalse(patch.clear);
        assertEquals(Set.of("#RadiationFill.Value", "#RadiationBarEffect.Value", "#RadiationBarEffect.Visible"), selectors(patch));
        assertTrue(Arrays.stream(patch.commands).allMatch(c -> c.type == CustomUICommandType.Set));
    }

    @Test
    void continuousChangesProduceAtMostFourPatchesPerSecond() {
        sync(true, 0.125f, Exposure.EXPOSED);
        packets.sent.clear();
        for (int i = 1; i <= 8; i++) {
            dose.setTotalRadiations(20 + i);
            sync(true, 0.125f, Exposure.EXPOSED);
        }
        assertEquals(4, packets.sent.size());
    }

    @Test
    void stageAndExposureChangesAreSentEvenWhenBarWidthDoesNotChange() {
        dose.setTotalRadiations(119.999f);
        sync(true, 0.25f, Exposure.EXPOSED);
        packets.sent.clear();
        dose.setTotalRadiations(120);
        sync(true, 0.25f, Exposure.SHELTERED);
        var patch = packets.last();
        assertEquals(Set.of("#RadiationStage.Text", "#RadiationExposure.Text"), selectors(patch));
        assertTrue(value(patch, "#RadiationStage.Text").contains("Poisoned"));
        dose.setTotalRadiations(119.999f);
        sync(true, 0.25f, Exposure.NIGHT);
        assertTrue(value(packets.last(), "#RadiationStage.Text").contains("Severe"));
    }

    @Test
    void subDisplayPrecisionChangesDoNotSendPackets() {
        sync(true, 0.25f, Exposure.UNKNOWN);
        packets.sent.clear();
        dose.setTotalRadiations(20.501f);
        sync(true, 0.25f, Exposure.UNKNOWN);
        assertTrue(packets.sent.isEmpty());
    }

    @Test
    void barFillsAcrossTheWholeDoseRangeAndDrainsDuringRecovery() {
        dose.setTotalRadiations(0);
        sync(true, 0.25f, Exposure.EXPOSED);
        assertEquals(0f, barValue(packets.last()));
        dose.setTotalRadiations(90);
        sync(true, 0.25f, Exposure.EXPOSED);
        assertEquals(0.5f, barValue(packets.last()));
        dose.setTotalRadiations(180);
        sync(true, 0.25f, Exposure.EXPOSED);
        assertEquals(1f, barValue(packets.last()));
        assertEquals(Set.of("#RadiationFill.Value", "#RadiationBarEffect.Value", "#RadiationStage.Text"), selectors(packets.last()));
        dose.setTotalRadiations(45);
        sync(true, 0.25f, Exposure.SHELTERED);
        assertEquals(0.25f, barValue(packets.last()), 0.002f);
        dose.setTotalRadiations(0);
        sync(true, 0.25f, Exposure.NIGHT);
        assertEquals(0f, barValue(packets.last()));
    }

    @Test
    void removalIsIdempotentPreservesOtherHudsAndAllowsFreshReattachment() {
        var other = new CustomUIHud(playerRef, "AnotherMod:Hud") {
            @Override protected void build(UICommandBuilder builder) {}
        };
        manager.addCustomHud(playerRef, other);
        sync(true, 0.25f, Exposure.NIGHT);
        var original = manager.getCustomHud(RadiationHud.KEY);
        packets.sent.clear();
        RadiationHud.remove(manager, playerRef);
        RadiationHud.remove(manager, playerRef);
        assertNull(manager.getCustomHud(RadiationHud.KEY));
        assertSame(other, manager.getCustomHud("AnotherMod:Hud"));
        assertEquals(1, packets.sent.size());
        assertTrue(packets.last().clear);
        assertNull(packets.last().commands);

        dose.setTotalRadiations(90);
        sync(false, 0.25f, Exposure.UNKNOWN);
        assertNull(manager.getCustomHud(RadiationHud.KEY));
        sync(true, 0.25f, Exposure.UNKNOWN);
        assertNotSame(original, manager.getCustomHud(RadiationHud.KEY));
        assertTrue(value(packets.last(), "#RadiationStage.Text").contains("Severe"));
    }

    @Test
    void recoveryTrailSettlesThenStopsSendingPackets() {
        dose.setTotalRadiations(90);
        sync(true, 0.25f, Exposure.SHELTERED);
        dose.setTotalRadiations(45);
        sync(true, 0.25f, Exposure.SHELTERED);
        assertEquals(0.25f, barValue(packets.last()), 0.002f);
        assertTrue(value(packets.last(), "#RadiationBarEffect.Visible").contains("true"));

        sync(true, 0.25f, Exposure.SHELTERED);
        float trail = effectValue(packets.last());
        assertTrue(trail > 0.25f && trail < 0.5f);
        sync(true, 0.25f, Exposure.SHELTERED);
        assertTrue(effectValue(packets.last()) < trail);
        sync(true, 0.25f, Exposure.SHELTERED);
        assertTrue(value(packets.last(), "#RadiationBarEffect.Visible").contains("false"));
        int settledPackets = packets.sent.size();
        for (int i = 0; i < 20; i++) sync(true, 0.25f, Exposure.SHELTERED);
        assertEquals(settledPackets, packets.sent.size());
    }

    @Test
    void ongoingRecoveryAdvancesTheTrailInsteadOfFreezingItAtTheOldDose() {
        dose.setTotalRadiations(180);
        sync(true, 0.25f, Exposure.NIGHT);
        dose.setTotalRadiations(120);
        sync(true, 0.25f, Exposure.NIGHT);
        dose.setTotalRadiations(60);
        sync(true, 0.25f, Exposure.NIGHT);
        float first = effectValue(packets.last());
        assertTrue(first < 1 && first > 0.33f);
        dose.setTotalRadiations(0);
        sync(true, 0.25f, Exposure.NIGHT);
        assertTrue(effectValue(packets.last()) < first);
    }

    @Test
    void deathCancelsAnActiveTrailImmediately() {
        sync(true, 0.25f, Exposure.SHELTERED);
        dose.setTotalRadiations(10);
        sync(true, 0.25f, Exposure.SHELTERED);
        assertTrue(value(packets.last(), "#RadiationBarEffect.Visible").contains("true"));
        dose.setTotalRadiations(0);
        sync(true, 0.25f, Exposure.DEAD);
        assertEquals(0, effectValue(packets.last()));
        assertTrue(value(packets.last(), "#RadiationBarEffect.Visible").contains("false"));
    }

    @Test
    void missingDoseRemovesHudAndNativeResetCanBeRecovered() {
        sync(true, 0.25f, Exposure.NIGHT);
        RadiationHud.sync(manager, playerRef, true, 0.25f, null, Exposure.UNKNOWN);
        assertNull(manager.getCustomHud(RadiationHud.KEY));
        sync(true, 0.25f, Exposure.NIGHT);
        manager.resetHud(playerRef);
        assertNull(manager.getCustomHud(RadiationHud.KEY));
        packets.sent.clear();
        sync(true, 0.25f, Exposure.NIGHT);
        assertEquals(1, packets.sent.size());
        assertTrue(packets.last().clear);
    }

    @Test
    void aNewPlayerReferenceGetsAFreshHudInsteadOfUpdatingTheOldConnection() {
        sync(true, 0.25f, Exposure.NIGHT);
        var nextPackets = new RecordingPackets();
        var nextPlayer = player(nextPackets);
        RadiationHud.sync(manager, nextPlayer, true, 0.25f, dose, Exposure.UNKNOWN);
        assertSame(nextPlayer, manager.getCustomHud(RadiationHud.KEY).getPlayerRef());
        assertTrue(nextPackets.last().clear);
        assertEquals(1, packets.sent.size(), "Do not send updates to a stale connection");
    }

    @Test
    void respawnAndExemptOrDisabledStatesRemainVisibleWithAccurateDose() {
        sync(true, 0.25f, Exposure.EXPOSED);
        for (Exposure exposure : new Exposure[]{Exposure.EXEMPT, Exposure.DISABLED, Exposure.DEAD}) {
            sync(true, 0.25f, exposure);
            assertTrue(value(packets.last(), "#RadiationExposure.Text").contains(
                    RadiationHudState.of(dose.getTotalRadiations(), exposure).exposureText()));
        }
        dose.setTotalRadiations(0);
        sync(true, 0.25f, Exposure.SHELTERED);
        assertEquals(0f, barValue(packets.last()));
        assertTrue(value(packets.last(), "#RadiationStage.Text").contains("Healthy"));
    }

    @Test
    void invalidTimestepsSendNothing() {
        for (float dt : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) sync(true, dt, Exposure.EXPOSED);
        assertTrue(packets.sent.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Exposure.class)
    void everyExposureStateHasAReadableLabel(Exposure exposure) {
        var state = RadiationHudState.of(4.5f, exposure);
        assertTrue(state.exposureText().startsWith("Exposure: "));
        assertFalse(state.exposureText().contains("_"));
        assertEquals(0, state.stage());
    }

    @Test
    void layoutIsPackagedAtThePathUsedByNativeHudCommands() throws IOException {
        try (var stream = getClass().getResourceAsStream("/Common/UI/Custom/" + RadiationHud.LAYOUT)) {
            assertNotNull(stream);
            var layout = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String id : new String[]{"RadiationStage", "RadiationExposure"}) {
                assertTrue(layout.contains("Label #" + id));
            }
        }
    }

    private void sync(boolean ready, float dt, Exposure exposure) {
        RadiationHud.sync(manager, playerRef, ready, dt, dose, exposure);
    }

    private static PlayerRef player(RecordingPackets packets) {
        return new PlayerRef(null, UUID.randomUUID(), "HUD test", "en-US", packets, null);
    }

    private static Set<String> selectors(CustomHud packet) {
        return Arrays.stream(packet.commands).map(c -> c.selector).collect(Collectors.toSet());
    }

    private static String value(CustomHud packet, String selector) {
        return Arrays.stream(packet.commands).filter(c -> selector.equals(c.selector)).findFirst().orElseThrow().data;
    }

    private static float barValue(CustomHud packet) {
        return (float) BsonDocument.parse(value(packet, "#RadiationFill.Value")).getNumber("0").doubleValue();
    }

    private static float effectValue(CustomHud packet) {
        return (float) BsonDocument.parse(value(packet, "#RadiationBarEffect.Value")).getNumber("0").doubleValue();
    }

    private static final class RecordingPackets extends PacketHandler {
        final List<ToClientPacket> sent = new ArrayList<>();
        RecordingPackets() { super(null, null); }
        @Override public String getIdentifier() { return "radiation-hud-test"; }
        @Override public void accept(ToServerPacket packet) { throw new UnsupportedOperationException(); }
        @Override public void writeNoCache(ToClientPacket packet) { sent.add(packet); }
        CustomHud last() { return (CustomHud) sent.getLast(); }
    }
}
