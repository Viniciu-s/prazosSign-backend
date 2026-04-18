package com.vinicius.prazos.documents.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentRequest(
	@NotBlank(message = "Título do documento é obrigatório")
	@Size(max = 255, message = "Título do documento deve ter no máximo 255 caracteres")
	String title,

	@NotNull(message = "Conteúdo do documento é obrigatório")
	String content
) {
}
