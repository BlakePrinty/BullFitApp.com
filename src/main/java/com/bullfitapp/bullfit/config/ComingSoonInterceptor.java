package com.bullfitapp.bullfit.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class ComingSoonInterceptor implements HandlerInterceptor {

    private final SiteAccess siteAccess;

    public ComingSoonInterceptor(SiteAccess siteAccess) {
        this.siteAccess = siteAccess;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!siteAccess.isLimitedNow()) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/welcome");
        return false;
    }
}
