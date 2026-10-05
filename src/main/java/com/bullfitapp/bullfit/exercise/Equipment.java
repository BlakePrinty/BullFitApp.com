package com.bullfitapp.bullfit.exercise;

public enum Equipment {
    MACHINE("Machine"),
    BARBELL("Barbell"),
    SMITH_MACHINE("Smith Machine"),
    DUMBBELL("Dumbbell"),
    CABLE("Cable"),
    FREEMOTION("Free Motion"),
    BODYWEIGHT_ONLY("Bodyweight Only"),
    BODYWEIGHT_LOADABLE("Bodyweight Loadable"),
    MACHINE_ASSISTANCE("Machine Assistance");

    private final String displayName;

    Equipment(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
