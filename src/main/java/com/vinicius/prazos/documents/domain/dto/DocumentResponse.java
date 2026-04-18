package com.vinicius.prazos.documents.domain.dto;

import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import java.time.Instant;

public record DocumentResponse(
	Long id,
	Long groupId,
	String title,
	String content,
	DocumentStatus status,
	Instant createdAt,
	Instant updatedAt
) {
}
