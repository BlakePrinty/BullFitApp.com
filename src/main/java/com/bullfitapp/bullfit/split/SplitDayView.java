package com.bullfitapp.bullfit.split;

import java.util.List;

public record SplitDayView(Long id, int dayNumber, String label, List<SplitExerciseView> exercises) {
    public String displayLabel() {
        return label == null ? "Day " + dayNumber : "Day " + dayNumber + ": " + label;
    }
}
