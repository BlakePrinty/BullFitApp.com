package com.bullfitapp.bullfit.exercise;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/exercises")
public class AdminExerciseController {

    private static final String LIST_URL = "/admin/exercises";

    private final ExerciseService exerciseService;

    public AdminExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("exercises", exerciseService.allMaster());
        return "exercise/admin-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ExerciseForm());
        ExerciseFormSupport.addFormAttributes(model, "Add exercise", LIST_URL + "/new", LIST_URL);
        return "exercise/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") ExerciseForm form,
                         BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                exerciseService.createMaster(form);
                return "redirect:" + LIST_URL;
            } catch (ExerciseException e) {
                ExerciseFormSupport.reject(result, e);
            }
        }
        ExerciseFormSupport.addFormAttributes(model, "Add exercise", LIST_URL + "/new", LIST_URL);
        return "exercise/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", ExerciseFormSupport.toForm(exerciseService.getMaster(id)));
        ExerciseFormSupport.addFormAttributes(model, "Edit exercise", LIST_URL + "/" + id + "/edit", LIST_URL);
        return "exercise/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ExerciseForm form,
                         BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                exerciseService.updateMaster(id, form);
                return "redirect:" + LIST_URL;
            } catch (ExerciseException e) {
                ExerciseFormSupport.reject(result, e);
            }
        }
        ExerciseFormSupport.addFormAttributes(model, "Edit exercise", LIST_URL + "/" + id + "/edit", LIST_URL);
        return "exercise/form";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id) {
        exerciseService.setMasterActive(id, false);
        return "redirect:" + LIST_URL;
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id) {
        exerciseService.setMasterActive(id, true);
        return "redirect:" + LIST_URL;
    }
}
