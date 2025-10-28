package com.task.reifensbank.service;

import com.task.reifensbank.model.LoginRequest;
import com.task.reifensbank.model.LoginResponse;
import com.task.reifensbank.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthService(AuthenticationManager authManager, JwtService jwt) {
        this.authManager = authManager;
        this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        String token = jwt.generate(authentication);
        return new LoginResponse()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwt.getExpirationSeconds());
    }
}
