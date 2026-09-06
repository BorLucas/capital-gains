package com.example.capitalgains.orders;

import com.example.capitalgains.domain.error.CapitalGainsException;

import java.util.UUID;

public class OrderNotFoundException extends CapitalGainsException {

    public OrderNotFoundException(UUID id) {
        super("order not found: " + id);
    }
}
