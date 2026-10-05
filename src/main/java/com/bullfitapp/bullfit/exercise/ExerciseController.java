package com.bullfitapp.bullfit.exercise;

import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final UserRepository userRepository;

    public ExerciseController(ExerciseService exerciseService, UserRepository userRepository) {
        this.exerciseService = exerciseService;
        this.userRepository = userRepository;
    }

    @GetMapping("/exercises")
    public String library(@RequestParam(required = false) String q,
                          @RequestParam(required = false) MuscleGroup muscle,
                          @RequestParam(required = false) Equipment equipment,
                          Principal principal, Model model) {
        Long userId = userRepository.findByUsername(principal.getName()).orElseThrow().getId();
        model.addAttribute("exercises", exerciseService.search(userId, q, muscle, equipment));
        model.addAttribute("muscleGroups", MuscleGroup.values());
        model.addAttribute("equipmentTypes", Equipment.values());
        model.addAttribute("q", q);
        model.addAttribute("selectedMuscle", muscle);
        model.addAttribute("selectedEquipment", equipment);
        return "exercise/library";
    }
}
