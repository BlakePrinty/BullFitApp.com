package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/runs")
public class SplitRunController {

    private final SplitRunService runService;
    private final UserRepository userRepository;

    public SplitRunController(SplitRunService runService, UserRepository userRepository) {
        this.runService = runService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{runId}/stop")
    public String stop(@PathVariable Long runId, Principal principal, RedirectAttributes redirect) {
        Long userId = userRepository.findByUsername(principal.getName()).orElseThrow().getId();
        try {
            runService.stop(userId, runId);
            redirect.addFlashAttribute("message", "BullSplit stopped.");
        } catch (SplitException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/workouts";
    }
}
