package com.example.capitalgains.domain;

/** How a brokerage fee is charged on an order. */
public enum FeeType {
    /** The same amount on every order. */
    FIXED,
    /** A percentage of the order's gross value. */
    PERCENT
}
