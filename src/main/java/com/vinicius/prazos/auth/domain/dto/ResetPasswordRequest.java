package com.vinicius.prazos.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
	@NotBlank(message = "Token é obrigatório")
	String token,
	@NotBlank(message = "Senha é obrigatória")
	@Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
	String newPassword
) {
}