package com.bank.ciftpin.exception;

public class CifNotFoundException extends RuntimeException {
    public CifNotFoundException(String cifNumber) {
        super("CIF not found: " + cifNumber);
    }
}