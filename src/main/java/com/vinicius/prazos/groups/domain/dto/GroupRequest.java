package com.vinicius.prazos.groups.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupRequest(
	@NotBlank(message = "Nome do grupo é obrigatório")
	@Size(max = 255, message = "Nome do grupo deve ter no máximo 255 caracteres")
	String name
) {
}