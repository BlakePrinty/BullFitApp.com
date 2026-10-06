package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.exercise.Equipment;
import com.bullfitapp.bullfit.exercise.MuscleGroup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PersonalRecord(Long exerciseId, String exerciseName, MuscleGroup muscleGroup,
                             Equipment equipment, BigDecimal weight, int reps,
                             LocalDateTime achievedAt, BigDecimal estimatedOneRepMax) {

    /** e.g. "225 lbs x 3", "15 reps", "+25 lbs x 5", "50 lbs assist x 6" */
    public String bestDisplay() { return describe(equipment, weight, reps); }

    public String e1rmDisplay() {
        return estimatedOneRepMax == null ? "-" : Numbers.plain(estimatedOneRepMax) + " lbs";
    }

    public static String describe(Equipment equipment, BigDecimal weight, int reps) {
        if (!equipment.usesWeight()) return reps + " reps";
        if (equipment == Equipment.MACHINE_ASSISTANCE) {
            return Numbers.plain(weight) + " lbs assist x " + reps;
        }
        if (equipment == Equipment.BODYWEIGHT_LOADABLE) {
            return (weight == null || weight.signum() == 0)
                    ? "Bodyweight x " + reps
                    : "+" + Numbers.plain(weight) + " lbs x " + reps;
        }
        return Numbers.plain(weight) + " lbs x " + reps;
    }
}
