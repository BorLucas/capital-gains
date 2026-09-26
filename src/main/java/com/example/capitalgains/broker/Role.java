package com.example.capitalgains.broker;

/** What a broker user may do. */
public enum Role {
    /** Trades and manages only their own account. */
    USER,
    /** Also manages users, the ticker catalog, fees and tax rules. */
    ADMIN
}
