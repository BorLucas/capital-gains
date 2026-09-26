package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

public class NotAuthenticatedException extends CapitalGainsException {

    public NotAuthenticatedException() {
        super("log in first");
    }
}
