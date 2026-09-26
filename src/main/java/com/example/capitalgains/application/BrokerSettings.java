package com.example.capitalgains.application;

import java.math.BigDecimal;

/** Per-user broker settings. The fee is charged on every order, buy or sell. */
public record BrokerSettings(BigDecimal brokerageFee) {
}
