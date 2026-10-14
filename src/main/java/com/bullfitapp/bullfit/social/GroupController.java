package com.bullfitapp.bullfit.social;

import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;

    public GroupController(GroupService groupService, UserRepository userRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    private User user(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow();
    }

    private String attempt(String target, RedirectAttributes redirect, String success, Runnable action) {
        try {
            action.run();
            if (success != null) redirect.addFlashAttribute("message", success);
        } catch (SocialException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + target;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        User user = user(principal);
        model.addAttribute("groups", groupService.myGroups(user.getId()));
        model.addAttribute("invites", groupService.myInvites(user.getId()));
        model.addAttribute("ownedCount", groupService.ownedCount(user.getId()));
        model.addAttribute("maxOwned", GroupLimits.describe(GroupLimits.maxOwned(user.getRole())));
        return "groups/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new GroupForm());
        return "groups/new";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") GroupForm form, BindingResult result,
                         Principal principal) {
        if (result.hasErrors()) {
            return "groups/new";
        }
        try {
            return "redirect:/groups/" + groupService.create(user(principal), form);
        } catch (SocialException e) {
            result.reject("group.invalid", e.getMessage());
            return "groups/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("group", groupService.view(id, user(principal)));
        return "groups/detail";
    }

    @PostMapping("/{id}/settings")
    public String settings(@PathVariable Long id, @RequestParam String name,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) Integer weeklyGoal,
                           Principal principal, RedirectAttributes redirect) {
        return attempt("/groups/" + id, redirect, "Saved.",
                () -> groupService.updateSettings(user(principal).getId(), id, name, description, weeklyGoal));
    }

    @PostMapping("/{id}/invite")
    public String invite(@PathVariable Long id, @RequestParam Long friendId,
                         Principal principal, RedirectAttributes redirect) {
        return attempt("/groups/" + id, redirect, "Invite sent.",
                () -> groupService.invite(user(principal).getId(), id, friendId));
    }

    @PostMapping("/{id}/members/{userId}/remove")
    public String remove(@PathVariable Long id, @PathVariable Long userId,
                         Principal principal, RedirectAttributes redirect) {
        return attempt("/groups/" + id, redirect, "Removed.",
                () -> groupService.removeMember(user(principal).getId(), id, userId));
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            groupService.acceptInvite(user(principal), id);
            redirect.addFlashAttribute("message", "You joined the group.");
            return "redirect:/groups/" + id;
        } catch (SocialException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/groups";
        }
    }

    @PostMapping("/{id}/decline")
    public String decline(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return attempt("/groups", redirect, "Invite declined.",
                () -> groupService.declineInvite(user(principal).getId(), id));
    }

    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            groupService.leave(user(principal).getId(), id);
            redirect.addFlashAttribute("message", "You left the group.");
            return "redirect:/groups";
        } catch (SocialException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/groups/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        groupService.delete(user(principal).getId(), id);
        redirect.addFlashAttribute("message", "Group deleted.");
        return "redirect:/groups";
    }
}
