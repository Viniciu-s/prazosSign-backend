package com.vinicius.prazos.signatures.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicSignatureSubmitRequest(
	@NotBlank(message = "Nome do signatário é obrigatório")
	@Size(max = 255, message = "Nome do signatário deve ter no máximo 255 caracteres")
	String name,

	@NotBlank(message = "Assinatura é obrigatória")
	String signature
) {
}
