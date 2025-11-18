package com.example.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DebugController {

    @GetMapping("/debug-auth")
    @ResponseBody
    public String debugAuth(Authentication authentication) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usuario: ").append(authentication.getName()).append("\n");
        sb.append("Authorities:\n");
        authentication.getAuthorities().forEach(authority -> {
            sb.append(" - ").append(authority.getAuthority()).append("\n");
        });
        return sb.toString();
    }



}

