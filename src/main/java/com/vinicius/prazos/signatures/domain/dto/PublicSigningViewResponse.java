package com.vinicius.prazos.signatures.domain.dto;

import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import java.time.Instant;

public record PublicSigningViewResponse(
	Long signerId,
	Long documentId,
	String documentTitle,
	String documentContent,
	String signerName,
	String signerEmail,
	Integer signingOrder,
	SignerStatus signerStatus,
	DocumentStatus documentStatus,
	Instant viewedAt,
	Instant signedAt,
	Instant tokenExpiresAt,
	Boolean signatureValid,
	boolean canSign
) {
}
