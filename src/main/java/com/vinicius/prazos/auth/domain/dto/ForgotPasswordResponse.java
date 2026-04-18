package com.vinicius.prazos.auth.domain.dto;

public record ForgotPasswordResponse(
	String message,
	String resetToken
) {
}