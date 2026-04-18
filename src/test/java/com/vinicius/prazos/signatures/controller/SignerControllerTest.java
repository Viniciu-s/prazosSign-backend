package com.vinicius.prazos.signatures.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vinicius.prazos.signatures.domain.dto.CreateSignerRequest;
import com.vinicius.prazos.signatures.domain.dto.SignerResponse;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import com.vinicius.prazos.signatures.service.SignerService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class SignerControllerTest {

	@Mock
	private SignerService signerService;

	@InjectMocks
	private SignerController signerController;

	private UserDetails userDetails;

	@BeforeEach
	void setUp() {
		userDetails = User.withUsername("user@example.com")
			.password("password")
			.authorities(List.of())
			.build();
	}

	@Test
	void shouldCreateSignerForAuthenticatedUser() {
		// Arrange
		CreateSignerRequest request = new CreateSignerRequest("Maria", "maria@example.com", 1);
		SignerResponse expectedResponse = new SignerResponse(
			1L,
			10L,
			"Maria",
			"maria@example.com",
			1,
			SignerStatus.PENDENTE,
			"token-1",
			"/sign/public/token-1",
			Instant.parse("2026-04-25T10:00:00Z"),
			null,
			null,
			null,
			Instant.parse("2026-04-18T10:00:00Z")
		);

		when(signerService.addSigner(10L, userDetails.getUsername(), request)).thenReturn(expectedResponse);

		// Act
		SignerResponse response = signerController.create(10L, userDetails, request);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(signerService).addSigner(10L, userDetails.getUsername(), request);
	}

	@Test
	void shouldListSignersForAuthenticatedUser() {
		// Arrange
		List<SignerResponse> expectedResponse = List.of(
			new SignerResponse(
				1L,
				10L,
				"Maria",
				"maria@example.com",
				null,
				SignerStatus.PENDENTE,
				"token-1",
				"/sign/public/token-1",
				Instant.parse("2026-04-25T10:00:00Z"),
				null,
				null,
				null,
				Instant.parse("2026-04-18T10:00:00Z")
			)
		);

		when(signerService.listSigners(10L, userDetails.getUsername())).thenReturn(expectedResponse);

		// Act
		List<SignerResponse> response = signerController.list(10L, userDetails);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(signerService).listSigners(10L, userDetails.getUsername());
	}
}
