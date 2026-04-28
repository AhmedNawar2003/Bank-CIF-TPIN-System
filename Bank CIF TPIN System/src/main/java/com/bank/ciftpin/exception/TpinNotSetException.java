package com.bank.ciftpin.exception;

public class TpinNotSetException extends RuntimeException {
    public TpinNotSetException(String cifNumber) {
        super("TPIN has not been set for CIF: " + cifNumber);
    }
}