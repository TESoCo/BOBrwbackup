package com.example.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorLogin {



    @GetMapping("/login")
    public String showLoginForm(Model model) {
        // You can add any model attributes if needed


        // For example: model.addAttribute("error", errorMessage);
        return "login"; // This should match your Thymeleaf template name
    }
}