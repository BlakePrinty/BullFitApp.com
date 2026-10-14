package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class FriendController {

    private final FriendService friendService;
    private final UserRepository userRepository;

    public FriendController(FriendService friendService, UserRepository userRepository) {
        this.friendService = friendService;
        this.userRepository = userRepository;
    }

    private Long userId(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow().getId();
    }

    /**
     * Only ever redirect to a few known local pages, so a crafted "next" can't send
     * people elsewhere.
     */
    private String back(String next) {
        boolean ok = next != null
                && (next.equals("/friends") || next.startsWith("/profile/") || next.startsWith("/people"))
                && !next.contains("//") && !next.contains("\\")
                && !next.contains("\r") && !next.contains("\n");
        return "redirect:" + (ok ? next : "/friends");
    }

    private String attempt(String next, RedirectAttributes redirect, String success, Runnable action) {
        try {
            action.run();
            if (success != null)
                redirect.addFlashAttribute("message", success);
        } catch (SocialException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return back(next);
    }

    @GetMapping("/friends")
    public String friends(Principal principal, Model model) {
        Long id = userId(principal);
        model.addAttribute("friends", friendService.friends(id));
        model.addAttribute("incoming", friendService.incoming(id));
        model.addAttribute("outgoing", friendService.outgoing(id));
        model.addAttribute("blocked", friendService.blocked(id));
        return "friends/index";
    }

    @GetMapping("/people")
    public String people(@RequestParam(required = false) String q, Principal principal, Model model) {
        String query = q == null ? "" : q.trim();
        model.addAttribute("q", query);
        model.addAttribute("tooShort", !query.isEmpty() && query.length() < 2);
        model.addAttribute("results", query.length() >= 2
                ? friendService.search(userId(principal), query)
                : List.of());
        return "friends/people";
    }

    @PostMapping("/friends/request")
    public String request(@RequestParam Long receiverId, @RequestParam(required = false) String next,
            Principal principal, RedirectAttributes redirect) {
        return attempt(next, redirect, "Done.",
                () -> friendService.sendRequest(userId(principal), receiverId));
    }

    @PostMapping("/friends/requests/{id}/accept")
    public String accept(@PathVariable Long id, @RequestParam(required = false) String next,
            Principal principal, RedirectAttributes redirect) {
        return attempt(next, redirect, "You're now friends.", () -> friendService.accept(userId(principal), id));
    }

    @PostMapping("/friends/requests/{id}/decline")
    public String decline(@PathVariable Long id, @RequestParam(required = false) String next,
            Principal principal, RedirectAttributes redirect) {
        return attempt(next, redirect, "Request declined.", () -> friendService.decline(userId(principal), id));
    }

    @PostMapping("/friends/requests/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String next,
            Principal principal, RedirectAttributes redirect) {
        return attempt(next, redirect, "Request cancelled.", () -> friendService.cancel(userId(principal), id));
    }

    @PostMapping("/friends/{otherId}/remove")
    public String remove(@PathVariable Long otherId, @RequestParam(required = false) String next,
            Principal principal, RedirectAttributes redirect) {
        return attempt(next, redirect, "Removed from your friends.",
                () -> friendService.unfriend(userId(principal), otherId));
    }

    @PostMapping("/friends/{otherId}/block")
    public String block(@PathVariable Long otherId, @RequestParam(required = false) String next,
            Principal principal, RedirectAttributes redirect) {
        return attempt(next, redirect, "Blocked.", () -> friendService.block(userId(principal), otherId));
    }

    @PostMapping("/friends/{otherId}/unblock")
    public String unblock(@PathVariable Long otherId, @RequestParam(required = false) String next,
            Principal principal, RedirectAttributes redirect) {
        return attempt(next, redirect, "Unblocked.", () -> friendService.unblock(userId(principal), otherId));
    }
}
