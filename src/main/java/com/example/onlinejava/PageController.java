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
}