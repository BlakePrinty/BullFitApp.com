package com.bullfitapp.bullfit.exercise;

import com.bullfitapp.bullfit.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/my-exercises")
public class MyExerciseController {

    private static final String LIST_URL = "/my-exercises";

    private final ExerciseService exerciseService;
    private final UserRepository userRepository;
    private final ExerciseRequestService requestService;

    public MyExerciseController(ExerciseService exerciseService,
                                ExerciseRequestService requestService,
                                UserRepository userRepository) {
        this.exerciseService = exerciseService;
        this.requestService = requestService;
        this.userRepository = userRepository;
    }

    private Long userId(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow().getId();
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Long userId = userId(principal);
        model.addAttribute("exercises", exerciseService.customFor(userId));
        model.addAttribute("activeCount", exerciseService.activeCustomCount(userId));
        model.addAttribute("maxCustom", ExerciseService.MAX_CUSTOM_EXERCISES);
        model.addAttribute("requests", requestService.forUser(userId));
        model.addAttribute("pendingIds", requestService.pendingExerciseIds(userId));
        return "exercise/my-list";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            exerciseService.setCustomActive(id, userId(principal), false);
        } catch (ExerciseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + LIST_URL;
    }

    @PostMapping("/{id}/request")
    public String submitRequest(@PathVariable Long id,
                                @RequestParam(required = false) String note,
                                Principal principal, RedirectAttributes redirect) {
        try {
            requestService.submit(userId(principal), id, note);
            redirect.addFlashAttribute("message", "Submitted for review.");
        } catch (ExerciseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + LIST_URL;
    }

    @PostMapping("/requests/{requestId}/withdraw")
    public String withdrawRequest(@PathVariable Long requestId, Principal principal,
                                  RedirectAttributes redirect) {
        try {
            requestService.withdraw(requestId, userId(principal));
            redirect.addFlashAttribute("message", "Request withdrawn.");
        } catch (ExerciseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + LIST_URL;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ExerciseForm());
        ExerciseFormSupport.addFormAttributes(model, "New custom exercise", LIST_URL + "/new", LIST_URL);
        return "exercise/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") ExerciseForm form,
                         BindingResult result, Principal principal, Model model) {
        if (!result.hasErrors()) {
            try {
                exerciseService.createCustom(userId(principal), form);
                return "redirect:" + LIST_URL;
            } catch (ExerciseException e) {
                ExerciseFormSupport.reject(result, e);
            }
        }
        ExerciseFormSupport.addFormAttributes(model, "New custom exercise", LIST_URL + "/new", LIST_URL);
        return "exercise/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        Exercise exercise = exerciseService.getCustom(id, userId(principal));
        model.addAttribute("form", ExerciseFormSupport.toForm(exercise));
        ExerciseFormSupport.addFormAttributes(model, "Edit custom exercise", LIST_URL + "/" + id + "/edit", LIST_URL);
        return "exercise/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ExerciseForm form,
                         BindingResult result, Principal principal, Model model) {
        if (!result.hasErrors()) {
            try {
                exerciseService.updateCustom(id, userId(principal), form);
                return "redirect:" + LIST_URL;
            } catch (ExerciseException e) {
                ExerciseFormSupport.reject(result, e);
            }
        }
        ExerciseFormSupport.addFormAttributes(model, "Edit custom exercise", LIST_URL + "/" + id + "/edit", LIST_URL);
        return "exercise/form";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            exerciseService.setCustomActive(id, userId(principal), true);
        } catch (ExerciseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + LIST_URL;
    }
}
