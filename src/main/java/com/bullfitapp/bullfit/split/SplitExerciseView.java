package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.exercise.Equipment;
import com.bullfitapp.bullfit.exercise.MuscleGroup;

public record SplitExerciseView(Long id, Long exerciseId, String name, MuscleGroup muscleGroup,
        Equipment equipment, boolean custom,
        Integer targetSets, String targetReps) {
    public String targetsDisplay() {
        if (targetSets != null && targetReps != null)
            return targetSets + " x " + targetReps;
        if (targetSets != null)
            return targetSets + " sets";
        if (targetReps != null)
            return targetReps + " reps";
        return "";
    }
}
