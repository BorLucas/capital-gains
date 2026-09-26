package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

/** Deliberately vague: it never says whether the username or the password was wrong. */
public class InvalidCredentialsException extends CapitalGainsException {

    public InvalidCredentialsException() {
        super("invalid username or password");
    }
}
