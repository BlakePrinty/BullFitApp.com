package com.bullfitapp.bullfit.split;

import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class BrowseController {

    private final BrowseService browseService;
    private final BullSplitService splitService;
    private final UserRepository userRepository;

    public BrowseController(BrowseService browseService, BullSplitService splitService,
            UserRepository userRepository) {
        this.browseService = browseService;
        this.splitService = splitService;
        this.userRepository = userRepository;
    }

    private User user(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow();
    }

    @GetMapping("/browse")
    public String browse(@RequestParam(required = false) Integer days,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page, Model model) {
        boolean popular = "popular".equals(sort);
        Page<BrowseRow> result = browseService.browse(days, q, popular, page);
        model.addAttribute("rows", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrevious", result.hasPrevious());
        model.addAttribute("total", result.getTotalElements());
        model.addAttribute("days", days);
        model.addAttribute("q", q);
        model.addAttribute("sort", popular ? "popular" : "newest");
        return "browse/index";
    }

    @GetMapping("/browse/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("pub", splitService.viewPublic(id, user(principal).getId()));
        return "browse/detail";
    }

    @PostMapping("/browse/{id}/save")
    public String save(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            splitService.saveCopy(user(principal), id);
            redirect.addFlashAttribute("message", "Saved to your plan.");
            return "redirect:/splits/saved";
        } catch (SplitException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/browse/" + id;
        }
    }
}
