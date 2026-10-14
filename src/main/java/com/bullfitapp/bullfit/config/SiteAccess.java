package com.bullfitapp.bullfit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SiteAccess {

    private final boolean comingSoon;

    public SiteAccess(@Value("${bullfit.coming-soon:false}") boolean comingSoon) {
        this.comingSoon = comingSoon;
    }

    public boolean isComingSoon() {
        return comingSoon;
    }

    /** True when coming-soon mode is on and this is a logged-in user who is not an admin or developer. */
    public boolean isLimited(Authentication auth) {
        if (!comingSoon) return false;
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_DEVELOPER"));
    }

    public boolean isLimitedNow() {
        return isLimited(SecurityContextHolder.getContext().getAuthentication());
    }
}
