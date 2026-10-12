package com.bullfitapp.bullfit.common;

import com.bullfitapp.bullfit.social.FriendService;
import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository userRepository;
    private final FriendService friendService;

    public GlobalModelAttributes(UserRepository userRepository, FriendService friendService) {
        this.userRepository = userRepository;
        this.friendService = friendService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }

    @ModelAttribute("pendingRequests")
    public Long pendingRequests(Principal principal) {
        if (principal == null) return 0L;
        return userRepository.findByUsername(principal.getName())
                .map(u -> friendService.pendingIncomingCount(u.getId())).orElse(0L);
    }
}