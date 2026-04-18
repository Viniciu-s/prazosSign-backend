package com.vinicius.prazos.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	@NotBlank(message = "Nome é obrigatório")
	String name,
	@NotBlank(message = "E-mail é obrigatório")
	@Email(message = "E-mail inválido")
	String email,
	@NotBlank(message = "Senha é obrigatória")
	@Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
	String password
) {
}