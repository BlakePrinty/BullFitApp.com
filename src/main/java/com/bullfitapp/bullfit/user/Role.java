package com.bullfitapp.bullfit.user;

public enum Role {
    USER("#FFFFFF", null),
    PREMIUM("#FFD700", null),
    ADMIN("#ff5e5e", "[ADMIN]"),
    DEVELOPER("#3bd441", "[DEVELOPER]");

    private final String color;
    private final String prefix;

    Role(String color, String prefix) {
        this.color = color;
        this.prefix = prefix;
    }

    public String getColor() {
        return color;
    }

    public String getPrefix() {
        return prefix;
    }
}
