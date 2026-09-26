package com.example.capitalgains.application;

import com.example.capitalgains.broker.UserAccount;

public record UserView(Long id, String username) {

    static UserView of(UserAccount account) {
        return new UserView(account.getId(), account.getUsername());
    }
}
