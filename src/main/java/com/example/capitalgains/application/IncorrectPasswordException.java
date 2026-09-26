package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

/** The current password given to confirm a sensitive change is wrong (the session itself is fine). */
public class IncorrectPasswordException extends CapitalGainsException {

    public IncorrectPasswordException() {
        super("current password is incorrect");
    }
}
