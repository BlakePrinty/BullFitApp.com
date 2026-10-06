package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Set;

@Controller
@RequestMapping("/records")
public class RecordsController {

    private final PersonalRecordService recordService;
    private final UserRepository userRepository;

    public RecordsController(PersonalRecordService recordService, UserRepository userRepository) {
        this.recordService = recordService;
        this.userRepository = userRepository;
    }

    private Long userId(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow().getId();
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Long userId = userId(principal);
        Set<Long> pinned = recordService.pinnedExerciseIds(userId);
        model.addAttribute("records", recordService.all(userId));
        model.addAttribute("pinnedIds", pinned);
        model.addAttribute("pinnedCount", pinned.size());
        model.addAttribute("maxPinned", PersonalRecordService.MAX_PINNED);
        return "workout/records";
    }

    @PostMapping("/{exerciseId}/pin")
    public String pin(@PathVariable Long exerciseId, Principal principal, RedirectAttributes redirect) {
        try {
            recordService.pin(userId(principal), exerciseId);
        } catch (WorkoutException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/records";
    }

    @PostMapping("/{exerciseId}/unpin")
    public String unpin(@PathVariable Long exerciseId, Principal principal) {
        recordService.unpin(userId(principal), exerciseId);
        return "redirect:/records";
    }
}
