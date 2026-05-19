package com.afetch.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProfileViewController {

    @GetMapping("/users/{username}")
    public String publicProfile(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        return "user-profile";
    }
}
