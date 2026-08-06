package com.example.onlinejava;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    @GetMapping("/user")
    public String user(
            @AuthenticationPrincipal OAuth2User user
    ) {
        return "Logged in as: " + user.getAttribute("login");
    }
}
