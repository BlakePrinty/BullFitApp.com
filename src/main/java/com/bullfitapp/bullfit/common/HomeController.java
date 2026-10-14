package com.bullfitapp.bullfit.common;

import com.bullfitapp.bullfit.config.SiteAccess;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final SiteAccess siteAccess;

    public HomeController(SiteAccess siteAccess) {
        this.siteAccess = siteAccess;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/welcome")
    public String welcome(@RequestParam(required = false) String registered, Model model) {
        if (!siteAccess.isComingSoon()) {
            return "redirect:/";
        }
        model.addAttribute("justRegistered", registered != null);
        return "welcome";
    }
}