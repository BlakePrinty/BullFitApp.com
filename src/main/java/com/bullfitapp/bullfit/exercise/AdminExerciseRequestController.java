package com.bullfitapp.bullfit.exercise;

import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/exercise-requests")
public class AdminExerciseRequestController {

    private static final String LIST_URL = "/admin/exercise-requests";

    private final ExerciseRequestService requestService;
    private final UserRepository userRepository;

    public AdminExerciseRequestController(ExerciseRequestService requestService,
                                          UserRepository userRepository) {
        this.requestService = requestService;
        this.userRepository = userRepository;
    }

    private Long userId(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow().getId();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("requests", requestService.pending());
        return "exercise/admin-requests";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            requestService.approve(id, userId(principal));
            redirect.addFlashAttribute("message", "Approved. The exercise is now in the master list.");
        } catch (ExerciseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + LIST_URL;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reviewNote,
                         Principal principal, RedirectAttributes redirect) {
        try {
            requestService.reject(id, userId(principal), reviewNote);
            redirect.addFlashAttribute("message", "Request rejected.");
        } catch (ExerciseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + LIST_URL;
    }
}
