package com.vinicius.prazos.signatures.domain.dto;

import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import java.time.Instant;

public record SignerResponse(
	Long id,
	Long documentId,
	String name,
	String email,
	Integer signingOrder,
	SignerStatus status,
	String token,
	String publicLink,
	Instant tokenExpiresAt,
	Instant viewedAt,
	Instant signedAt,
	Boolean signatureValid,
	Instant createdAt
) {
}
