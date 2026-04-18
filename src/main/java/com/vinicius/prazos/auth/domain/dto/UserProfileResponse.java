package com.vinicius.prazos.auth.domain.dto;

import java.time.Instant;

public record UserProfileResponse(
	Long id,
	String name,
	String email,
	Instant createdAt,
	Instant updatedAt
) {
}