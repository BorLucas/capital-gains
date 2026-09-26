package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

public class UsernameTakenException extends CapitalGainsException {

    public UsernameTakenException(String username) {
        super("username already taken: " + username);
    }
}
