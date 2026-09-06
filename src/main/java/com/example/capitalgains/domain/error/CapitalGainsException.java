package com.example.capitalgains.domain.error;

/**
 * Root of the project's business-error hierarchy. Every domain exception
 * inherits from here, which allows uniform handling at the edge (web/CLI)
 * without relying on a generic {@link IllegalArgumentException}.
 */
public abstract class CapitalGainsException extends RuntimeException {

    protected CapitalGainsException(String message) {
        super(message);
    }
}
