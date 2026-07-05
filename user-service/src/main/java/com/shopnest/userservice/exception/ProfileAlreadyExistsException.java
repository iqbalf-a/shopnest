package com.shopnest.userservice.exception;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException(String message) {
        super(message);
    }
}
