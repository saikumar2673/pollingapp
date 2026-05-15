package com.example.polling.common;

import com.example.polling.user.AppUserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {
    private final AppUserRepository users;

    public GlobalModelAttributes(AppUserRepository users) {
        this.users = users;
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return users.findByEmail(authentication.getName())
            .map(user -> user.getName())
            .orElse(authentication.getName());
    }
}
