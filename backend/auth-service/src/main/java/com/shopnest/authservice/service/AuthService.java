package com.shopnest.authservice.service;

import com.shopnest.authservice.dto.request.LoginRequest;
import com.shopnest.authservice.dto.request.RegisterRequest;
import com.shopnest.authservice.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
