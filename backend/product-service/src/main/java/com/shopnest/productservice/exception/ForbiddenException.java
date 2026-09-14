package com.shopnest.productservice.exception;

// Dilempar saat identitas pemanggil sah, tapi haknya tidak cukup (403, bukan 401)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
