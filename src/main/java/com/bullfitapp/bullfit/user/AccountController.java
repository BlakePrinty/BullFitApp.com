package com.bullfitapp.bullfit.user;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/settings/password")
    public String form(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        return "user/change-password";
    }

    @PostMapping("/settings/password")
    public String change(@Valid @ModelAttribute("form") ChangePasswordForm form,
                         BindingResult result, Principal principal) {
        if (form.getNewPassword() != null
                && !form.getNewPassword().equals(form.getConfirmNewPassword())) {
            result.rejectValue("confirmNewPassword", "mismatch", "Passwords do not match");
        }
        if (result.hasErrors()) {
            return "user/change-password";
        }
        try {
            userService.changePassword(principal.getName(),
                    form.getCurrentPassword(), form.getNewPassword());
        } catch (IllegalArgumentException e) {
            result.rejectValue("currentPassword", "incorrect", "Current password is incorrect");
            return "user/change-password";
        }
        return "redirect:/settings/password?changed";
    }
}
