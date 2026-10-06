package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.exercise.Equipment;
import com.bullfitapp.bullfit.exercise.MuscleGroup;

import java.util.List;
import java.util.stream.Collectors;

public record WorkoutExerciseView(Long id, Long exerciseId, String name,
                                  MuscleGroup muscleGroup, Equipment equipment, boolean cardio,
                                  List<SetView> sets, List<CardioView> cardioEntries) {

    public String setsSummary() {
        return sets.stream().map(s -> {
            if (!equipment.usesWeight()) return s.reps() + " reps";
            if (equipment == Equipment.MACHINE_ASSISTANCE) return s.weightDisplay() + " assist x " + s.reps();
            if (equipment == Equipment.BODYWEIGHT_LOADABLE
                    && s.weight() != null && s.weight().signum() == 0) return "BW x " + s.reps();
            return s.weightDisplay() + " x " + s.reps();
        }).collect(Collectors.joining(", "));
    }

    public String cardioSummary() {
        return cardioEntries.stream().map(c -> {
            String text = c.durationDisplay();
            if (c.distance() != null) text += ", " + c.distanceDisplay() + " mi";
            return text;
        }).collect(Collectors.joining(" | "));
    }
}
