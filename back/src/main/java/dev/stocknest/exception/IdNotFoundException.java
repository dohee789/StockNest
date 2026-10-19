package dev.stocknest.exception;

public class IdNotFoundException extends RuntimeException {
    public IdNotFoundException(String errMsg) {
        super(errMsg);
    }
}
