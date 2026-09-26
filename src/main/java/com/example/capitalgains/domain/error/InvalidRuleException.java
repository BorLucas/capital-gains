package com.example.capitalgains.domain.error;

/** A tax rule or fee schedule outside its allowed range (e.g. a tax rate above 100%). */
public class InvalidRuleException extends CapitalGainsException {

    public InvalidRuleException(String message) {
        super(message);
    }
}
