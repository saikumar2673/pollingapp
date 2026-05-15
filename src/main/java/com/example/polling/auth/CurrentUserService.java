package com.example.polling.auth;

import com.example.polling.common.NotFoundException;
import com.example.polling.user.AppUser;
import com.example.polling.user.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    public AppUser requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException("Please sign in to continue.");
        }
        return users.findByEmail(authentication.getName())
            .orElseThrow(() -> new NotFoundException("Signed-in user was not found."));
    }
}
