package com.vinicius.prazos.signatures.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.signatures.domain.dto.CreateSignerRequest;
import com.vinicius.prazos.signatures.domain.dto.PublicSignatureSubmitRequest;
import com.vinicius.prazos.signatures.domain.dto.PublicSignatureSubmitResponse;
import com.vinicius.prazos.signatures.domain.dto.PublicSigningViewResponse;
import com.vinicius.prazos.signatures.domain.dto.SignerResponse;
import com.vinicius.prazos.signatures.domain.entity.Signer;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import com.vinicius.prazos.signatures.domain.enums.SignatureLogEvent;
import com.vinicius.prazos.signatures.repository.SignerRepository;
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
class SignerServiceTest {

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private SignerRepository signerRepository;

	@Mock
	private CustomUserDetailsService userDetailsService;

	@Mock
	private SignatureLogService signatureLogService;

	@Mock
	private SignatureValidator signatureValidator;

	@InjectMocks
	private SignerService signerService;

	private User user;
	private Document document;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(1L);
		user.setEmail("user@example.com");

		document = new Document();
		document.setId(10L);
		document.setUser(user);
		document.setTitle("Contrato");
		document.setContent("Conteúdo");
		document.setStatus(DocumentStatus.RASCUNHO);
		document.setCreatedAt(Instant.parse("2026-04-18T10:00:00Z"));
		document.setUpdatedAt(Instant.parse("2026-04-18T10:00:00Z"));
	}

	@Test
	void shouldAddSignerToDraftDocument() {
		// Arrange
		CreateSignerRequest request = new CreateSignerRequest("  Maria Silva  ", "  MARIA@example.com  ", null);
		ArgumentCaptor<Signer> signerCaptor = ArgumentCaptor.forClass(Signer.class);

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(document.getId(), user.getId())).thenReturn(Optional.of(document));
		when(signerRepository.findAllByDocumentId(document.getId())).thenReturn(List.of());
		when(signerRepository.existsByDocumentIdAndEmailIgnoreCase(document.getId(), "maria@example.com")).thenReturn(false);
		when(signerRepository.save(any(Signer.class))).thenAnswer(invocation -> {
			Signer signer = invocation.getArgument(0);
			signer.setId(99L);
			signer.setCreatedAt(Instant.parse("2026-04-18T11:00:00Z"));
			return signer;
		});

		// Act
		SignerResponse response = signerService.addSigner(document.getId(), user.getEmail(), request);

		// Assert
		verify(signerRepository).save(signerCaptor.capture());
		assertThat(signerCaptor.getValue().getName()).isEqualTo("Maria Silva");
		assertThat(signerCaptor.getValue().getEmail()).isEqualTo("maria@example.com");
		assertThat(signerCaptor.getValue().getStatus()).isEqualTo(SignerStatus.PENDENTE);
		assertThat(response.id()).isEqualTo(99L);
		assertThat(response.publicLink()).contains("/sign/public/");
		verify(signatureLogService).log(document, signerCaptor.getValue(), SignatureLogEvent.SIGNATARIO_ADICIONADO, "Signatário adicionado ao documento", null, null);
	}

	@Test
	void shouldRejectMixedSigningOrderConfiguration() {
		// Arrange
		Signer existingSigner = buildSigner(50L, "joao@example.com", 1, SignerStatus.PENDENTE, null);

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(documentRepository.findByIdAndUserId(document.getId(), user.getId())).thenReturn(Optional.of(document));
		when(signerRepository.existsByDocumentIdAndEmailIgnoreCase(document.getId(), "maria@example.com")).thenReturn(false);
		when(signerRepository.findAllByDocumentId(document.getId())).thenReturn(List.of(existingSigner));

		// Act / Assert
		assertThatThrownBy(() -> signerService.addSigner(document.getId(), user.getEmail(), new CreateSignerRequest("Maria", "maria@example.com", null)))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(exception.getReason()).isEqualTo("Para usar ordem de assinatura, todos os signatários do documento precisam seguir o mesmo padrão");
			});
	}

	@Test
	void shouldRegisterViewAndMarkSignerAsViewed() {
		// Arrange
		document.setStatus(DocumentStatus.AGUARDANDO_ASSINATURA);
		Signer signer = buildSigner(50L, "maria@example.com", null, SignerStatus.PENDENTE, "public-token");
		when(signerRepository.findByToken("public-token")).thenReturn(Optional.of(signer));
		when(signerRepository.findAllByDocumentId(document.getId())).thenReturn(List.of(signer));
		when(signerRepository.save(any(Signer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		PublicSigningViewResponse response = signerService.registerView("public-token", "127.0.0.1", "JUnit");

		// Assert
		assertThat(response.canSign()).isTrue();
		assertThat(response.signerStatus()).isEqualTo(SignerStatus.VISUALIZADO);
		assertThat(response.viewedAt()).isNotNull();
		verify(signatureLogService).log(document, signer, SignatureLogEvent.LINK_VISUALIZADO, "Link de assinatura visualizado", "127.0.0.1", "JUnit");
	}

	@Test
	void shouldSubmitSignatureAndValidateDocument() {
		// Arrange
		document.setStatus(DocumentStatus.AGUARDANDO_ASSINATURA);
		Signer signer = buildSigner(50L, "maria@example.com", null, SignerStatus.PENDENTE, "public-token");

		when(signerRepository.findByToken("public-token")).thenReturn(Optional.of(signer));
		when(signerRepository.findAllByDocumentId(document.getId())).thenReturn(List.of(signer));
		when(signerRepository.save(any(Signer.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(documentRepository.save(document)).thenReturn(document);
		when(signatureValidator.generateSignatureHash(eq(document), any(Signer.class))).thenReturn("hash");
		when(signatureValidator.isValid(eq(document), any(Signer.class))).thenReturn(true);

		// Act
		PublicSignatureSubmitResponse response = signerService.submitSignature(
			"public-token",
			new PublicSignatureSubmitRequest("Maria Silva", "assinatura-base64"),
			"127.0.0.1",
			"JUnit"
		);

		// Assert
		assertThat(response.signerStatus()).isEqualTo(SignerStatus.ASSINADO);
		assertThat(response.documentStatus()).isEqualTo(DocumentStatus.VALIDADO);
		assertThat(response.signatureValid()).isTrue();
		assertThat(signer.getSignedAt()).isNotNull();
		assertThat(signer.getSignatureHash()).isEqualTo("hash");
		verify(signatureLogService).log(document, signer, SignatureLogEvent.ASSINATURA_ENVIADA, "Assinatura registrada com sucesso", "127.0.0.1", "JUnit");
		verify(signatureLogService).log(document, signer, SignatureLogEvent.ASSINATURA_VALIDADA, "Assinatura validada com sucesso", "127.0.0.1", "JUnit");
	}

	@Test
	void shouldRejectSubmitWhenItIsNotSignerTurn() {
		// Arrange
		document.setStatus(DocumentStatus.AGUARDANDO_ASSINATURA);
		Signer firstSigner = buildSigner(40L, "joao@example.com", 1, SignerStatus.PENDENTE, "first-token");
		Signer secondSigner = buildSigner(50L, "maria@example.com", 2, SignerStatus.AGUARDANDO_ORDEM, "second-token");

		when(signerRepository.findByToken("second-token")).thenReturn(Optional.of(secondSigner));
		when(signerRepository.findAllByDocumentId(document.getId())).thenReturn(List.of(firstSigner, secondSigner));

		// Act / Assert
		assertThatThrownBy(() -> signerService.submitSignature(
			"second-token",
			new PublicSignatureSubmitRequest("Maria", "assinatura"),
			"127.0.0.1",
			"JUnit"
		))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getReason()).isEqualTo("Ainda não é a vez desse signatário assinar");
			});
	}

	private Signer buildSigner(Long id, String email, Integer signingOrder, SignerStatus status, String token) {
		Signer signer = new Signer();
		signer.setId(id);
		signer.setDocument(document);
		signer.setName("Nome Original");
		signer.setEmail(email);
		signer.setSigningOrder(signingOrder);
		signer.setStatus(status);
		signer.setToken(token == null ? "token-" + id : token);
		signer.setTokenExpiresAt(Instant.now().plusSeconds(3600));
		signer.setCreatedAt(Instant.parse("2026-04-18T11:00:00Z"));
		return signer;
	}
}
