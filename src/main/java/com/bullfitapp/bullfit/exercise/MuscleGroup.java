package com.bullfitapp.bullfit.exercise;

public enum MuscleGroup {
    CHEST("Chest"), BACK("Back"), SHOULDERS("Shoulders"),
    BICEPS("Biceps"), TRICEPS("Triceps"), QUADS("Quads"),
    HAMSTRINGS("Hamstrings"), GLUTES("Glutes"), CALVES("Calves"),
    CORE("Core"), FULL_BODY("Full Body"), CARDIO("Cardio");

    private final String displayName;

    MuscleGroup(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
