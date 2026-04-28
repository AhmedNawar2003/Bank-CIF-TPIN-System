package com.bank.ciftpin.exception;

public class CifAlreadyExistsException extends RuntimeException {
    public CifAlreadyExistsException(String message) {
        super(message);
    }
}