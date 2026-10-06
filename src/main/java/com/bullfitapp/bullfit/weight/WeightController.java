package com.bullfitapp.bullfit.weight;

import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/weight")
public class WeightController {

    private static final int TABLE_LIMIT = 60;

    private final WeightService weightService;
    private final UserRepository userRepository;

    public WeightController(WeightService weightService, UserRepository userRepository) {
        this.weightService = weightService;
        this.userRepository = userRepository;
    }

    private User user(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow();
    }

    @GetMapping
    public String page(Principal principal, Model model) {
        User user = user(principal);
        List<BodyWeightLog> entries = weightService.entriesOldestFirst(user.getId());
        LocalDate today = WeightService.todayFor(user.getTimeZone());

        List<BodyWeightLog> newestFirst = new ArrayList<>(entries);
        Collections.reverse(newestFirst);

        model.addAttribute("today", today);
        model.addAttribute("todayEpoch", today.toEpochDay());
        model.addAttribute("epochDays", entries.stream().map(e -> e.getLoggedOn().toEpochDay()).toList());
        model.addAttribute("weights", entries.stream().map(e -> e.getWeight().doubleValue()).toList());
        model.addAttribute("entries", newestFirst.stream().limit(TABLE_LIMIT).toList());
        model.addAttribute("totalEntries", entries.size());
        model.addAttribute("tableLimit", TABLE_LIMIT);
        return "weight/index";
    }

    @PostMapping
    public String log(@RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                      @RequestParam(required = false) BigDecimal weight,
                      Principal principal, RedirectAttributes redirect) {
        User user = user(principal);
        try {
            weightService.log(user.getId(), user.getTimeZone(), date, weight);
            redirect.addFlashAttribute("message", "Saved.");
        } catch (WeightException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/weight";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal) {
        weightService.delete(user(principal).getId(), id);
        return "redirect:/weight";
    }
}
