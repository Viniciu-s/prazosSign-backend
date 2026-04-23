package com.vinicius.prazos.groups.domain.dto;

import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import java.time.Instant;

public record GroupDocumentResponse(
	Long id,
	String title,
	DocumentStatus status,
	Instant createdAt,
	Instant updatedAt
) {
}
