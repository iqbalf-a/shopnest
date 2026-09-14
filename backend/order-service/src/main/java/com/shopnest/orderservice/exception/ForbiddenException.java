package com.shopnest.orderservice.exception;

// Dilempar saat identitas pemanggil sah, tapi pesanan itu bukan miliknya (403, bukan 401)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
