package dev.stocknest.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String errMsg) {
        super(errMsg);
    }
}
