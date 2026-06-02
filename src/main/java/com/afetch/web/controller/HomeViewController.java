package com.afetch.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeViewController {

    @GetMapping({"/", "/home", "/feed", "/chats", "/assistant", "/profile", "/users/search"})
    public String app() {
        return "index";
    }

    @GetMapping("/favicon.ico")
    public String favicon() {
        return "forward:/images/logo.png";
    }
}
