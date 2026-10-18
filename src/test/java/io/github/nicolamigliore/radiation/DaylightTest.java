package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class DaylightTest {
    private static final Instant MIDNIGHT = Instant.parse("0001-01-01T00:00:00Z");

    @Test
    void sunriseStartsExposureAndSunsetEndsIt() {
        var time = new WorldTimeResource();
        int sunrise = WorldTimeResource.SUNRISE_SECONDS;
        int sunset = sunrise + WorldTimeResource.DAYTIME_SECONDS;
        time.setGameTime0(MIDNIGHT.plusSeconds(sunrise - 1));
        assertFalse(Daylight.isDay(time));
        time.setGameTime0(MIDNIGHT.plusSeconds(sunrise));
        assertTrue(Daylight.isDay(time));
        time.setGameTime0(MIDNIGHT.plusSeconds(sunset - 1));
        assertTrue(Daylight.isDay(time));
        time.setGameTime0(MIDNIGHT.plusSeconds(sunset));
        assertFalse(Daylight.isDay(time));
    }

    @Test
    void worldClocksStayIndependentAcrossTimeSkips() {
        var first = new WorldTimeResource();
        var second = new WorldTimeResource();
        first.setGameTime0(MIDNIGHT.plusSeconds(WorldTimeResource.SECONDS_PER_DAY / 2));
        second.setGameTime0(MIDNIGHT);
        assertTrue(Daylight.isDay(first));
        assertFalse(Daylight.isDay(second));
        first.setGameTime0(MIDNIGHT.plusSeconds(WorldTimeResource.SECONDS_PER_DAY));
        assertFalse(Daylight.isDay(first));
        second.setGameTime0(MIDNIGHT.plusSeconds(WorldTimeResource.SECONDS_PER_DAY / 2));
        assertTrue(Daylight.isDay(second));
        assertFalse(Daylight.isDay(first));
    }
}
