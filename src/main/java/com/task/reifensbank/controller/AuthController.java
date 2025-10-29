package com.task.reifensbank.controller;

import com.task.reifensbank.api.AuthApi;
import com.task.reifensbank.model.LoginRequest;
import com.task.reifensbank.model.LoginResponse;
import com.task.reifensbank.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<LoginResponse> authLogin(LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
}
