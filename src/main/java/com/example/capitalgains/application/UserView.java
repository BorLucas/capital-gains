package com.example.capitalgains.application;

import com.example.capitalgains.broker.Role;
import com.example.capitalgains.broker.UserAccount;

import java.time.Instant;

public record UserView(Long id, String username, Role role, Instant memberSince) {

    static UserView of(UserAccount account) {
        return new UserView(account.getId(), account.getUsername(), account.getRole(), account.getCreatedAt());
    }
}
