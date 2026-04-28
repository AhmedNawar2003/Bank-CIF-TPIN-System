package com.bank.ciftpin.exception;

public class CifBlockedException extends RuntimeException {
    public CifBlockedException(String cifNumber) {
        super("CIF is blocked due to 3 consecutive failed authentication attempts: " + cifNumber);
    }
}