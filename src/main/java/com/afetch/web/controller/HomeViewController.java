package com.afetch.web.controller;

import com.afetch.security.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeViewController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("username", SecurityUtils.currentUser().getUsername());
        return "home";
    }

    @GetMapping("/feed")
    public String feed() {
        return "feed";
    }

    @GetMapping("/chats")
    public String chats() {
        return "chats";
    }

    @GetMapping("/assistant")
    public String assistant() {
        return "assistant";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/users/search")
    public String searchUsers() {
        return "users-search";
    }
}
