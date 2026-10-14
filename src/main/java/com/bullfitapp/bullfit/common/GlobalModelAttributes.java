package com.bullfitapp.bullfit.common;

import com.bullfitapp.bullfit.config.SiteAccess;
import com.bullfitapp.bullfit.social.FriendService;
import com.bullfitapp.bullfit.social.GroupService;
import com.bullfitapp.bullfit.user.User;
import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository userRepository;
    private final FriendService friendService;
    private final GroupService groupService;
    private final SiteAccess siteAccess;

    public GlobalModelAttributes(UserRepository userRepository, FriendService friendService,
            GroupService groupService, SiteAccess siteAccess) {
        this.userRepository = userRepository;
        this.friendService = friendService;
        this.groupService = groupService;
        this.siteAccess = siteAccess;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null)
            return null;
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }

    @ModelAttribute("pendingRequests")
    public Long pendingRequests(Principal principal) {
        if (principal == null)
            return 0L;
        return userRepository.findByUsername(principal.getName())
                .map(u -> friendService.pendingIncomingCount(u.getId())).orElse(0L);
    }

    @ModelAttribute("pendingInvites")
    public Long pendingInvites(Principal principal) {
        if (principal == null)
            return 0L;
        return userRepository.findByUsername(principal.getName())
                .map(u -> groupService.pendingInviteCount(u.getId())).orElse(0L);
    }

    @ModelAttribute("comingSoon")
    public boolean comingSoon() {
        return siteAccess.isComingSoon();
    }

    @ModelAttribute("limitedAccess")
    public boolean limitedAccess() {
        return siteAccess.isLimitedNow();
    }
}