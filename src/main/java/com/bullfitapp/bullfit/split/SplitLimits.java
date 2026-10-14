package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.user.Role;

public final class SplitLimits {
    private SplitLimits() {
    }

    public static int maxPublished(Role role) {
        return switch (role) {
            case USER -> 3;
            case PREMIUM -> 12;
            case ADMIN, DEVELOPER -> Integer.MAX_VALUE;
        };
    }

    public static int maxHeld(Role role) {
        return switch (role) {
            case USER -> 5;
            case PREMIUM -> 20;
            case ADMIN, DEVELOPER -> Integer.MAX_VALUE;
        };
    }

    public static String describe(int limit) {
        return limit == Integer.MAX_VALUE ? "unlimited" : String.valueOf(limit);
    }
}
