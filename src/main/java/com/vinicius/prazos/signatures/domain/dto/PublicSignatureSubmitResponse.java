package com.vinicius.prazos.signatures.domain.dto;

import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import java.time.Instant;

public record PublicSignatureSubmitResponse(
	Long signerId,
	Long documentId,
	SignerStatus signerStatus,
	DocumentStatus documentStatus,
	Instant signedAt,
	boolean signatureValid
) {
}
