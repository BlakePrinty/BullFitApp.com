package com.bullfitapp.bullfit.common;

import java.time.ZoneId;
import java.util.List;

public final class ZoneOptions {

    public static final String DEFAULT = "America/New_York";

    private static final List<String> PREFIXES = List.of("America/", "Europe/", "Asia/",
            "Australia/", "Africa/", "Pacific/", "Atlantic/", "Indian/");

    private ZoneOptions() {}

    public static List<String> all() {
        return ZoneId.getAvailableZoneIds().stream()
                .filter(id -> PREFIXES.stream().anyMatch(id::startsWith))
                .sorted()
                .toList();
    }

    public static boolean isValid(String id) {
        return id != null && ZoneId.getAvailableZoneIds().contains(id);
    }
}
