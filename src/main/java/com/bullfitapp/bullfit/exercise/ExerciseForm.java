package com.bullfitapp.bullfit.exercise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ExerciseForm {

    @NotBlank(message = "Name is required")
    @Size(max = 80, message = "Name can be up to 80 characters")
    private String name;

    @NotNull(message = "Choose a muscle group")
    private MuscleGroup muscleGroup;

    @NotNull(message = "Choose equipment")
    private Equipment equipment;

    // getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MuscleGroup getMuscleGroup() {
        return muscleGroup;
    }

    public void setMuscleGroup(MuscleGroup muscleGroup) {
        this.muscleGroup = muscleGroup;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }
}
