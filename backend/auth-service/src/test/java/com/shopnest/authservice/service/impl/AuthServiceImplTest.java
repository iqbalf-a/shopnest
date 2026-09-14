package com.shopnest.authservice.service.impl;

import com.shopnest.authservice.dto.request.LoginRequest;
import com.shopnest.authservice.dto.request.RegisterRequest;
import com.shopnest.authservice.dto.response.AuthResponse;
import com.shopnest.authservice.entity.Role;
import com.shopnest.authservice.entity.User;
import com.shopnest.authservice.exception.EmailAlreadyExistsException;
import com.shopnest.authservice.repository.UserRepository;
import com.shopnest.authservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_success_returnsAuthResponseWithToken() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Iqbal");
        request.setEmail("iqbal@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals(request.getName(), response.getName());
        assertEquals(Role.USER.name(), response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Iqbal");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_success_returnsAuthResponseWithToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("iqbal@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .email(request.getEmail())
                .name("Iqbal")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getName(), response.getName());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
