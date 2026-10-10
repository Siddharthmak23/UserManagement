package com.usermanagement.controller;

import com.usermanagement.security.AppUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticatedUser(authentication)) {
            return "redirect:/home";
        }
        return "login";
    }

    @GetMapping("/login.jsp")
    public String loginAlias() {
        return "redirect:/login";
    }

    @GetMapping({"/", "/home"})
    public String home(Authentication authentication, Model model) {
        AppUserPrincipal principal = requirePrincipal(authentication);
        model.addAttribute("adminName", principal.getName());
        model.addAttribute("adminEmail", principal.getEmail());
        model.addAttribute("currentUserId", principal.getId());
        return "home";
    }

    @GetMapping("/forbidden")
    public String forbidden(Authentication authentication, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            model.addAttribute("adminName", principal.getName());
            model.addAttribute("adminEmail", principal.getEmail());
        }
        return "forbidden";
    }

    private static boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() instanceof AppUserPrincipal;
    }

    private static AppUserPrincipal requirePrincipal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal;
        }
        throw new IllegalStateException("An authenticated administrator is required");
    }
}
