package com.vinicius.prazos.signatures.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.repository.UserRepository;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.signatures.domain.entity.SignatureLog;
import com.vinicius.prazos.signatures.domain.entity.Signer;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import com.vinicius.prazos.signatures.domain.enums.SignatureLogEvent;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SignatureLogRepositoryTest {

	@Autowired
	private SignatureLogRepository signatureLogRepository;

	@Autowired
	private SignerRepository signerRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		signatureLogRepository.deleteAll();
		signerRepository.deleteAll();
		documentRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldDeleteLogsByDocumentId() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		Document firstDocument = documentRepository.save(buildDocument(owner, "Contrato A"));
		Document secondDocument = documentRepository.save(buildDocument(owner, "Contrato B"));

		Signer signer = signerRepository.save(buildSigner(firstDocument, "maria@example.com", "token-1"));
		signatureLogRepository.save(buildLog(firstDocument, signer, SignatureLogEvent.SIGNATARIO_ADICIONADO));
		signatureLogRepository.save(buildLog(firstDocument, signer, SignatureLogEvent.DOCUMENTO_ENVIADO));
		signatureLogRepository.save(buildLog(secondDocument, null, SignatureLogEvent.DOCUMENTO_ENVIADO));
		signatureLogRepository.flush();
		entityManager.clear();

		// Act
		signatureLogRepository.deleteByDocumentId(firstDocument.getId());
		entityManager.flush();
		entityManager.clear();

		// Assert
		assertThat(signatureLogRepository.findAll())
			.hasSize(1)
			.extracting(log -> log.getDocument().getId())
			.containsExactly(secondDocument.getId());
	}

	private User buildUser(String email, String name) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		return user;
	}

	private Document buildDocument(User user, String title) {
		Document document = new Document();
		document.setUser(user);
		document.setTitle(title);
		document.setContent("Conteudo");
		document.setStatus(DocumentStatus.RASCUNHO);
		return document;
	}

	private Signer buildSigner(Document document, String email, String token) {
		Signer signer = new Signer();
		signer.setDocument(document);
		signer.setName("Maria");
		signer.setEmail(email);
		signer.setStatus(SignerStatus.PENDENTE);
		signer.setToken(token);
		signer.setTokenExpiresAt(Instant.parse("2026-04-25T10:00:00Z"));
		return signer;
	}

	private SignatureLog buildLog(Document document, Signer signer, SignatureLogEvent event) {
		SignatureLog log = new SignatureLog();
		log.setDocument(document);
		log.setSigner(signer);
		log.setEvent(event);
		log.setDescription("Evento de teste");
		log.setIpAddress("127.0.0.1");
		log.setUserAgent("JUnit");
		return log;
	}
}
