package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

/** A logged-in user without the ADMIN role tried an admin action. */
public class AdminOnlyException extends CapitalGainsException {

    public AdminOnlyException() {
        super("only an admin can do this");
    }
}
