package com.vinicius.prazos.signatures.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSignerRequest(
	@NotBlank(message = "Nome do signatário é obrigatório")
	@Size(max = 255, message = "Nome do signatário deve ter no máximo 255 caracteres")
	String name,

	@NotBlank(message = "E-mail do signatário é obrigatório")
	@Email(message = "E-mail do signatário deve ser válido")
	@Size(max = 255, message = "E-mail do signatário deve ter no máximo 255 caracteres")
	String email,

	@Positive(message = "Ordem de assinatura deve ser maior que zero")
	Integer signingOrder
) {
}
