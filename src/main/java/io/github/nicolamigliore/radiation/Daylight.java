package io.github.nicolamigliore.radiation;

import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;

/** Use Hytale's solar schedule in each world's game clock, independent of weather. */
public final class Daylight {
    private Daylight() {}

    public static boolean isDay(WorldTimeResource time) {
        int second = time.getGameDateTime().toLocalTime().toSecondOfDay();
        int sunrise = WorldTimeResource.SUNRISE_SECONDS;
        int sunset = sunrise + WorldTimeResource.DAYTIME_SECONDS;
        return second >= sunrise && second < sunset;
    }
}
