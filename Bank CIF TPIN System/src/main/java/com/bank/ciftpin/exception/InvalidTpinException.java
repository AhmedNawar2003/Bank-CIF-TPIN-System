package com.bank.ciftpin.exception;

public class InvalidTpinException extends RuntimeException {
    public InvalidTpinException(int remainingAttempts) {
        super("Invalid TPIN. Remaining attempts: " + remainingAttempts);
    }
}