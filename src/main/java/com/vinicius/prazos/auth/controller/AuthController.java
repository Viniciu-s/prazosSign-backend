package com.vinicius.prazos.auth.controller;

import com.vinicius.prazos.auth.domain.dto.AuthResponse;
import com.vinicius.prazos.auth.domain.dto.ForgotPasswordRequest;
import com.vinicius.prazos.auth.domain.dto.ForgotPasswordResponse;
import com.vinicius.prazos.auth.domain.dto.LoginRequest;
import com.vinicius.prazos.auth.domain.dto.RegisterRequest;
import com.vinicius.prazos.auth.domain.dto.ResetPasswordRequest;
import com.vinicius.prazos.auth.domain.dto.ResetPasswordResponse;
import com.vinicius.prazos.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/forgot-password")
	public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		return authService.forgotPassword(request);
	}

	@PostMapping("/reset-password")
	public ResetPasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		return authService.resetPassword(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
		if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new IllegalArgumentException("Cabeçalho Authorization inválido");
		}

		authService.logout(authorizationHeader.substring(BEARER_PREFIX.length()));
	}
}