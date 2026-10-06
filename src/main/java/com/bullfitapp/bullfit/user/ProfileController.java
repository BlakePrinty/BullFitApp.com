package com.bullfitapp.bullfit.user;

import com.bullfitapp.bullfit.common.ZoneOptions;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final UserService userService;

    public ProfileController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String myProfile(Principal principal) {
        return "redirect:/profile/" + principal.getName();
    }

    @GetMapping("/profile/{username}")
    public String viewProfile(@PathVariable String username, Principal principal, Model model) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("profileUser", user);
        model.addAttribute("isOwner", user.getUsername().equals(principal.getName()));
        return "user/profile";
    }

    @GetMapping("/settings/profile")
    public String editForm(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        EditProfileForm form = new EditProfileForm();
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setBio(user.getBio());
        form.setWeight(user.getWeight());
        if (user.getHeight() != null) {
            int total = user.getHeight().intValue();
            form.setHeightFeet(total / 12);
            form.setHeightInches(total % 12);
        }
        form.setTimeZone(user.getTimeZone());
        model.addAttribute("zones", ZoneOptions.all());
        model.addAttribute("form", form);
        return "user/edit-profile";
    }

    @PostMapping("/settings/profile")
    public String saveProfile(@Valid @ModelAttribute("form") EditProfileForm form,
                              BindingResult result, Principal principal, Model model) {
        if (result.hasErrors()) {
            return "user/edit-profile";
        }

        model.addAttribute("zones", ZoneOptions.all());

        userService.updateProfile(principal.getName(), form);
        return "redirect:/profile";
    }
}
