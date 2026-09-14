package com.shopnest.userservice.exception;

// Dilempar saat identitas pemanggil sah, tapi profil/alamat itu bukan miliknya (403, bukan 401)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
