package com.vinicius.prazos.signatures.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.signatures.domain.dto.PublicSignatureSubmitRequest;
import com.vinicius.prazos.signatures.domain.dto.PublicSignatureSubmitResponse;
import com.vinicius.prazos.signatures.domain.dto.PublicSigningViewResponse;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import com.vinicius.prazos.signatures.service.SignerService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicSigningControllerTest {

	@Mock
	private SignerService signerService;

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private PublicSigningController publicSigningController;

	@Test
	void shouldRegisterPublicView() {
		// Arrange
		PublicSigningViewResponse expectedResponse = new PublicSigningViewResponse(
			1L,
			10L,
			"Contrato",
			"Conteúdo",
			"Maria",
			"maria@example.com",
			null,
			SignerStatus.VISUALIZADO,
			DocumentStatus.AGUARDANDO_ASSINATURA,
			Instant.parse("2026-04-18T10:00:00Z"),
			null,
			Instant.parse("2026-04-25T10:00:00Z"),
			null,
			true
		);

		when(request.getRemoteAddr()).thenReturn("127.0.0.1");
		when(request.getHeader("User-Agent")).thenReturn("JUnit");
		when(signerService.registerView("token-1", "127.0.0.1", "JUnit")).thenReturn(expectedResponse);

		// Act
		PublicSigningViewResponse response = publicSigningController.view("token-1", request);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(signerService).registerView("token-1", "127.0.0.1", "JUnit");
	}

	@Test
	void shouldSubmitPublicSignature() {
		// Arrange
		PublicSignatureSubmitRequest body = new PublicSignatureSubmitRequest("Maria", "assinatura");
		PublicSignatureSubmitResponse expectedResponse = new PublicSignatureSubmitResponse(
			1L,
			10L,
			SignerStatus.ASSINADO,
			DocumentStatus.VALIDADO,
			Instant.parse("2026-04-18T10:05:00Z"),
			true
		);

		when(request.getRemoteAddr()).thenReturn("127.0.0.1");
		when(request.getHeader("User-Agent")).thenReturn("JUnit");
		when(signerService.submitSignature("token-1", body, "127.0.0.1", "JUnit")).thenReturn(expectedResponse);

		// Act
		PublicSignatureSubmitResponse response = publicSigningController.submit("token-1", body, request);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(signerService).submitSignature("token-1", body, "127.0.0.1", "JUnit");
	}
}
