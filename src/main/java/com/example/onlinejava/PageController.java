package com.example.onlinejava;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/sandbox")
    public String sandbox(
            @AuthenticationPrincipal OAuth2User user,
            Model model
    ) {
        model.addAttribute(
                "username",
                user.getAttribute("login")
        );

        return "sandbox";
    }

    @GetMapping("/problems")
    public String problems() {
        return "problems";
    }

    @GetMapping("/problems/arrays")
    public String arrays() {
        return "problems-arrays";
    }

    @GetMapping("/problems/collections")
    public String collections() {
        return "problems-collections";
    }

    @GetMapping("/problems/algorithms")
    public String algorithms() {
        return "problems-algorithms";
    }

    @GetMapping("/problems/arrays/bubble-sort")
    public String bubbleSort() {
        return "bubble-sort";
    }
}