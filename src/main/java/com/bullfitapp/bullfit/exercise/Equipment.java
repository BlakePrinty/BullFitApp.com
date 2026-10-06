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

    public boolean usesWeight() { return this != BODYWEIGHT_ONLY; }

    public boolean weightRequired() { return usesWeight() && this != BODYWEIGHT_LOADABLE; }

    public String getWeightLabel() {
        return switch (this) {
            case MACHINE_ASSISTANCE -> "Assistance (lbs)";
            case BODYWEIGHT_LOADABLE -> "Added weight (lbs, optional)";
            default -> "Weight (lbs)";
        };
    }
}
