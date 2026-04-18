package com.vinicius.prazos.documents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.documents.domain.dto.DocumentMoveRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentResponse;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.groups.domain.entity.Group;
import com.vinicius.prazos.groups.repository.GroupRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private GroupRepository groupRepository;

	@Mock
	private CustomUserDetailsService userDetailsService;

	@InjectMocks
	private DocumentService documentService;

	private User user;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(1L);
		user.setEmail("user@example.com");
		user.setName("User");
	}

	@Test
	void shouldListDocumentsWithoutFilter() {
		// Arrange
		Document firstDocument = buildDocument(2L, "Contrato B", "Conteúdo B", DocumentStatus.AGUARDANDO_ASSINATURA, Instant.parse("2026-04-18T10:00:00Z"));
		Document secondDocument = buildDocument(1L, "Contrato A", "Conteúdo A", DocumentStatus.RASCUNHO, Instant.parse("2026-04-17T09:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId())).thenReturn(List.of(firstDocument, secondDocument));

		// Act
		List<DocumentResponse> response = documentService.listDocuments(user.getEmail(), null);

		// Assert
		assertThat(response)
			.extracting(DocumentResponse::title)
			.containsExactly("Contrato B", "Contrato A");
		assertThat(response)
			.extracting(DocumentResponse::status)
			.containsExactly(DocumentStatus.AGUARDANDO_ASSINATURA, DocumentStatus.RASCUNHO);
	}

	@Test
	void shouldListDocumentsWithStatusFilter() {
		// Arrange
		Document draftDocument = buildDocument(1L, "Rascunho", "Conteúdo", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T10:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), DocumentStatus.RASCUNHO))
			.thenReturn(List.of(draftDocument));

		// Act
		List<DocumentResponse> response = documentService.listDocuments(user.getEmail(), "rascunho");

		// Assert
		assertThat(response).hasSize(1);
		assertThat(response.getFirst().status()).isEqualTo(DocumentStatus.RASCUNHO);
	}

	@Test
	void shouldRejectInvalidStatusFilter() {
		// Arrange
		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);

		// Act / Assert
		assertThatThrownBy(() -> documentService.listDocuments(user.getEmail(), "invalid"))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(exception.getReason()).isEqualTo("Status do documento inválido");
			});
	}

	@Test
	void shouldGetOwnedDocument() {
		// Arrange
		Document document = buildDocument(1L, "Contrato", "Conteúdo", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T10:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));

		// Act
		DocumentResponse response = documentService.getDocument(1L, user.getEmail());

		// Assert
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.title()).isEqualTo("Contrato");
	}

	@Test
	void shouldCreateDraftDocument() {
		// Arrange
		DocumentRequest request = new DocumentRequest("  Contrato  ", "Conteúdo inicial");
		ArgumentCaptor<Document> savedDocumentCaptor = ArgumentCaptor.forClass(Document.class);

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
			Document document = invocation.getArgument(0);
			document.setId(10L);
			document.setCreatedAt(Instant.parse("2026-04-18T12:00:00Z"));
			document.setUpdatedAt(Instant.parse("2026-04-18T12:00:00Z"));
			return document;
		});

		// Act
		DocumentResponse response = documentService.createDocument(user.getEmail(), request);

		// Assert
		verify(documentRepository).save(savedDocumentCaptor.capture());
		assertThat(savedDocumentCaptor.getValue().getUser()).isSameAs(user);
		assertThat(savedDocumentCaptor.getValue().getTitle()).isEqualTo("Contrato");
		assertThat(savedDocumentCaptor.getValue().getStatus()).isEqualTo(DocumentStatus.RASCUNHO);
		assertThat(response.status()).isEqualTo(DocumentStatus.RASCUNHO);
	}

	@Test
	void shouldUpdateDraftDocument() {
		// Arrange
		Document document = buildDocument(1L, "Título antigo", "Conteúdo antigo", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T10:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));
		when(documentRepository.save(document)).thenReturn(document);

		// Act
		DocumentResponse response = documentService.updateDocument(1L, user.getEmail(), new DocumentRequest("  Novo título  ", "Novo conteúdo"));

		// Assert
		assertThat(document.getTitle()).isEqualTo("Novo título");
		assertThat(document.getContent()).isEqualTo("Novo conteúdo");
		assertThat(response.title()).isEqualTo("Novo título");
		verify(documentRepository).save(document);
	}

	@Test
	void shouldRejectEditingSentDocument() {
		// Arrange
		Document document = buildDocument(1L, "Contrato", "Conteúdo", DocumentStatus.AGUARDANDO_ASSINATURA, Instant.parse("2026-04-18T10:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));

		// Act / Assert
		assertThatThrownBy(() -> documentService.updateDocument(1L, user.getEmail(), new DocumentRequest("Novo", "Conteúdo")))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(exception.getReason()).isEqualTo("Documento enviado não pode ser editado");
			});
	}

	@Test
	void shouldDeleteOwnedDocument() {
		// Arrange
		Document document = buildDocument(1L, "Contrato", "Conteúdo", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T10:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));

		// Act
		documentService.deleteDocument(1L, user.getEmail());

		// Assert
		verify(documentRepository).delete(document);
	}

	@Test
	void shouldSendDraftDocument() {
		// Arrange
		Document document = buildDocument(1L, "Contrato", "Conteúdo", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T10:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));
		when(documentRepository.save(document)).thenReturn(document);

		// Act
		DocumentResponse response = documentService.sendDocument(1L, user.getEmail());

		// Assert
		assertThat(document.getStatus()).isEqualTo(DocumentStatus.AGUARDANDO_ASSINATURA);
		assertThat(response.status()).isEqualTo(DocumentStatus.AGUARDANDO_ASSINATURA);
		verify(documentRepository).save(document);
	}

	@Test
	void shouldRejectSendingAlreadySentDocument() {
		// Arrange
		Document document = buildDocument(1L, "Contrato", "Conteúdo", DocumentStatus.AGUARDANDO_ASSINATURA, Instant.parse("2026-04-18T10:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));

		// Act / Assert
		assertThatThrownBy(() -> documentService.sendDocument(1L, user.getEmail()))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(exception.getReason()).isEqualTo("Documento já foi enviado");
			});
	}

	@Test
	void shouldMoveDocumentToOwnedGroup() {
		// Arrange
		Document document = buildDocument(1L, "Contrato", "Conteúdo", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T10:00:00Z"));
		Group group = new Group();
		group.setId(7L);
		group.setUser(user);
		group.setName("Financeiro");

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));
		when(groupRepository.findByIdAndUserId(7L, user.getId())).thenReturn(Optional.of(group));
		when(documentRepository.save(document)).thenReturn(document);

		// Act
		DocumentResponse response = documentService.moveDocument(1L, user.getEmail(), new DocumentMoveRequest(7L));

		// Assert
		assertThat(document.getGroup()).isSameAs(group);
		assertThat(response.groupId()).isEqualTo(7L);
		verify(documentRepository).save(document);
	}

	@Test
	void shouldMoveDocumentBackToHome() {
		// Arrange
		Document document = buildDocument(1L, "Contrato", "Conteúdo", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T10:00:00Z"));
		Group currentGroup = new Group();
		currentGroup.setId(7L);
		document.setGroup(currentGroup);

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));
		when(documentRepository.save(document)).thenReturn(document);

		// Act
		DocumentResponse response = documentService.moveDocument(1L, user.getEmail(), new DocumentMoveRequest(null));

		// Assert
		assertThat(document.getGroup()).isNull();
		assertThat(response.groupId()).isNull();
		verify(documentRepository).save(document);
	}

	@Test
	void shouldRejectMoveToMissingGroup() {
		// Arrange
		Document document = buildDocument(1L, "Contrato", "Conteúdo", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T10:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(document));
		when(groupRepository.findByIdAndUserId(7L, user.getId())).thenReturn(Optional.empty());

		// Act / Assert
		assertThatThrownBy(() -> documentService.moveDocument(1L, user.getEmail(), new DocumentMoveRequest(7L)))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(exception.getReason()).isEqualTo("Grupo não encontrado");
			});
	}

	private Document buildDocument(Long id, String title, String content, DocumentStatus status, Instant updatedAt) {
		Document document = new Document();
		document.setId(id);
		document.setTitle(title);
		document.setContent(content);
		document.setStatus(status);
		document.setCreatedAt(updatedAt.minusSeconds(3600));
		document.setUpdatedAt(updatedAt);
		return document;
	}
}
