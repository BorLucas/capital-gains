package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

public class UserNotFoundException extends CapitalGainsException {

    public UserNotFoundException(Long id) {
        super("user not found: " + id);
    }
}
