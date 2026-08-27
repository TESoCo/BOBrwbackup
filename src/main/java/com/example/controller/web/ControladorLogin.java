package com.example.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ControladorLogin {



    @GetMapping("/login")
    public String showLoginForm(Model model) {




        return "login"; // This should match your Thymeleaf template name
    }
}