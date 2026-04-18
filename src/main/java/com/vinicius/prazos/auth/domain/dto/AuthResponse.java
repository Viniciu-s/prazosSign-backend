package com.vinicius.prazos.auth.domain.dto;

public record AuthResponse(
	String accessToken,
	String tokenType,
	long expiresIn,
	UserProfileResponse user
) {
}