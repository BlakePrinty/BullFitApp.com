package com.bullfitapp.bullfit.common;

import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository userRepository;

    public GlobalModelAttributes(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }
}