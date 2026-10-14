package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.Role;

public final class GroupLimits {
    private GroupLimits() {}

    public static int maxOwned(Role role) {
        return switch (role) {
            case USER -> 2;
            case PREMIUM -> 10;
            case ADMIN, DEVELOPER -> Integer.MAX_VALUE;
        };
    }

    public static int maxMembers(Role ownerRole) {
        return switch (ownerRole) {
            case USER -> 10;
            case PREMIUM -> 25;
            case ADMIN, DEVELOPER -> Integer.MAX_VALUE;
        };
    }

    public static int maxJoined(Role role) {
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
