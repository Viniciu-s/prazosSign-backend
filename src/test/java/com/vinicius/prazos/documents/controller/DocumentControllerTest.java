package com.vinicius.prazos.documents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vinicius.prazos.documents.domain.dto.DocumentMoveRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentResponse;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.service.DocumentService;
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
class DocumentControllerTest {

	@Mock
	private DocumentService documentService;

	@InjectMocks
	private DocumentController documentController;

	private UserDetails userDetails;

	@BeforeEach
	void setUp() {
		userDetails = User.withUsername("user@example.com")
			.password("password")
			.authorities(List.of())
			.build();
	}

	@Test
	void shouldListDocumentsForAuthenticatedUser() {
		// Arrange
		List<DocumentResponse> expectedDocuments = List.of(
			new DocumentResponse(1L, null, "Contrato A", "Conteúdo A", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T08:00:00Z"), Instant.parse("2026-04-18T09:00:00Z")),
			new DocumentResponse(2L, 3L, "Contrato B", "Conteúdo B", DocumentStatus.AGUARDANDO_ASSINATURA, Instant.parse("2026-04-17T08:00:00Z"), Instant.parse("2026-04-18T08:00:00Z"))
		);

		when(documentService.listDocuments(userDetails.getUsername(), "RASCUNHO")).thenReturn(expectedDocuments);

		// Act
		List<DocumentResponse> response = documentController.list(userDetails, "RASCUNHO");

		// Assert
		assertThat(response).isEqualTo(expectedDocuments);
		verify(documentService).listDocuments(userDetails.getUsername(), "RASCUNHO");
	}

	@Test
	void shouldGetDocumentForAuthenticatedUser() {
		// Arrange
		DocumentResponse expectedResponse = new DocumentResponse(
			1L,
			null,
			"Contrato",
			"Conteúdo",
			DocumentStatus.RASCUNHO,
			Instant.parse("2026-04-18T08:00:00Z"),
			Instant.parse("2026-04-18T09:00:00Z")
		);

		when(documentService.getDocument(1L, userDetails.getUsername())).thenReturn(expectedResponse);

		// Act
		DocumentResponse response = documentController.get(1L, userDetails);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(documentService).getDocument(1L, userDetails.getUsername());
	}

	@Test
	void shouldCreateDocumentForAuthenticatedUser() {
		// Arrange
		DocumentRequest request = new DocumentRequest("Contrato", "Conteúdo");
		DocumentResponse expectedResponse = new DocumentResponse(
			1L,
			null,
			"Contrato",
			"Conteúdo",
			DocumentStatus.RASCUNHO,
			Instant.parse("2026-04-18T08:00:00Z"),
			Instant.parse("2026-04-18T08:00:00Z")
		);

		when(documentService.createDocument(userDetails.getUsername(), request)).thenReturn(expectedResponse);

		// Act
		DocumentResponse response = documentController.create(userDetails, request);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(documentService).createDocument(userDetails.getUsername(), request);
	}

	@Test
	void shouldUpdateDocumentForAuthenticatedUser() {
		// Arrange
		DocumentRequest request = new DocumentRequest("Atualizado", "Novo conteúdo");
		DocumentResponse expectedResponse = new DocumentResponse(
			1L,
			null,
			"Atualizado",
			"Novo conteúdo",
			DocumentStatus.RASCUNHO,
			Instant.parse("2026-04-18T08:00:00Z"),
			Instant.parse("2026-04-18T10:00:00Z")
		);

		when(documentService.updateDocument(1L, userDetails.getUsername(), request)).thenReturn(expectedResponse);

		// Act
		DocumentResponse response = documentController.update(1L, userDetails, request);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(documentService).updateDocument(1L, userDetails.getUsername(), request);
	}

	@Test
	void shouldDeleteDocumentForAuthenticatedUser() {
		// Arrange

		// Act
		assertThatCode(() -> documentController.delete(1L, userDetails)).doesNotThrowAnyException();

		// Assert
		verify(documentService).deleteDocument(1L, userDetails.getUsername());
	}

	@Test
	void shouldSendDocumentForAuthenticatedUser() {
		// Arrange
		DocumentResponse expectedResponse = new DocumentResponse(
			1L,
			null,
			"Contrato",
			"Conteúdo",
			DocumentStatus.AGUARDANDO_ASSINATURA,
			Instant.parse("2026-04-18T08:00:00Z"),
			Instant.parse("2026-04-18T10:00:00Z")
		);

		when(documentService.sendDocument(1L, userDetails.getUsername())).thenReturn(expectedResponse);

		// Act
		DocumentResponse response = documentController.send(1L, userDetails);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(documentService).sendDocument(1L, userDetails.getUsername());
	}

	@Test
	void shouldMoveDocumentForAuthenticatedUser() {
		// Arrange
		DocumentMoveRequest request = new DocumentMoveRequest(5L);
		DocumentResponse expectedResponse = new DocumentResponse(
			1L,
			5L,
			"Contrato",
			"Conteúdo",
			DocumentStatus.RASCUNHO,
			Instant.parse("2026-04-18T08:00:00Z"),
			Instant.parse("2026-04-18T10:00:00Z")
		);

		when(documentService.moveDocument(1L, userDetails.getUsername(), request)).thenReturn(expectedResponse);

		// Act
		DocumentResponse response = documentController.move(1L, userDetails, request);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(documentService).moveDocument(1L, userDetails.getUsername(), request);
	}
}
