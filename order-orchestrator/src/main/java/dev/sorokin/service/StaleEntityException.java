package dev.sorokin.service;

public class StaleEntityException extends RuntimeException {

    public StaleEntityException(String message) {
        super(message);
    }
}
