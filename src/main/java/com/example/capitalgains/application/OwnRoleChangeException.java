package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;

/**
 * An admin tried to change their own role. Refusing it is what guarantees there
 * is always at least one admin: only admins change roles, and never their own.
 */
public class OwnRoleChangeException extends CapitalGainsException {

    public OwnRoleChangeException() {
        super("you cannot change your own role");
    }
}
