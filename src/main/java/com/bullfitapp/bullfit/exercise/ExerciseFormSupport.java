package com.bullfitapp.bullfit.exercise;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

final class ExerciseFormSupport {

    private ExerciseFormSupport() {}

    static void addFormAttributes(Model model, String heading, String action, String cancelUrl) {
        model.addAttribute("heading", heading);
        model.addAttribute("formAction", action);
        model.addAttribute("cancelUrl", cancelUrl);
        model.addAttribute("muscleGroups", MuscleGroup.values());
        model.addAttribute("equipmentTypes", Equipment.values());
    }

    static void reject(BindingResult result, ExerciseException e) {
        if (e.getField() == null) {
            result.reject("exercise.invalid", e.getMessage());
        } else {
            result.rejectValue(e.getField(), "exercise.invalid", e.getMessage());
        }
    }

    static ExerciseForm toForm(Exercise exercise) {
        ExerciseForm form = new ExerciseForm();
        form.setName(exercise.getName());
        form.setMuscleGroup(exercise.getMuscleGroup());
        form.setEquipment(exercise.getEquipment());
        return form;
    }
}
