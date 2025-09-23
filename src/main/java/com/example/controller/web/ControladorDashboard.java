package com.example.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;

@Controller
public class ControladorDashboard {

    @GetMapping("/redirigir")
    public String redirectToDashboard() {
        // Since your dashboard uses Thymeleaf security tags to show/hide content
        // based on permissions, you can simply redirect everyone to the dashboard
        // and let the frontend handle what they can see

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Optional: Log the redirection for debugging
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

            System.out.println("User " + username + " with roles " + authorities + " redirected to dashboard");
        }

        return "redirect:/dashboard";
    }
}