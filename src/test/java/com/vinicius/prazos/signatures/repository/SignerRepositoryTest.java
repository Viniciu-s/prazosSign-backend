package com.vinicius.prazos.signatures.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.repository.UserRepository;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.signatures.domain.entity.Signer;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SignerRepositoryTest {

	@Autowired
	private SignerRepository signerRepository;

	@Autowired
	private SignatureLogRepository signatureLogRepository;

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
	void shouldFindAllByDocumentId() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		Document firstDocument = documentRepository.save(buildDocument(owner, "Contrato A"));
		Document secondDocument = documentRepository.save(buildDocument(owner, "Contrato B"));

		Signer firstSigner = signerRepository.save(buildSigner(firstDocument, "Maria", "maria@example.com", "token-1"));
		Signer secondSigner = signerRepository.save(buildSigner(firstDocument, "Joao", "joao@example.com", "token-2"));
		signerRepository.save(buildSigner(secondDocument, "Ana", "ana@example.com", "token-3"));
		signerRepository.flush();
		entityManager.clear();

		// Act
		List<Signer> signers = signerRepository.findAllByDocumentId(firstDocument.getId());

		// Assert
		assertThat(signers)
			.extracting(Signer::getId)
			.containsExactly(firstSigner.getId(), secondSigner.getId());
		assertThat(signers)
			.extracting(Signer::getEmail)
			.containsExactly("maria@example.com", "joao@example.com");
	}

	@Test
	void shouldCheckIfSignerExistsByDocumentIdAndEmailIgnoringCase() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		Document document = documentRepository.save(buildDocument(owner, "Contrato"));
		signerRepository.saveAndFlush(buildSigner(document, "Maria", "maria@example.com", "token-1"));
		entityManager.clear();

		// Act
		boolean exists = signerRepository.existsByDocumentIdAndEmailIgnoreCase(document.getId(), "MARIA@EXAMPLE.COM");
		boolean missing = signerRepository.existsByDocumentIdAndEmailIgnoreCase(document.getId(), "joao@example.com");

		// Assert
		assertThat(exists).isTrue();
		assertThat(missing).isFalse();
	}

	@Test
	void shouldFindSignerByToken() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		Document document = documentRepository.save(buildDocument(owner, "Contrato"));
		Signer signer = signerRepository.saveAndFlush(buildSigner(document, "Maria", "maria@example.com", "public-token"));
		entityManager.clear();

		// Act
		Optional<Signer> foundSigner = signerRepository.findByToken("public-token");
		Optional<Signer> missingSigner = signerRepository.findByToken("missing-token");

		// Assert
		assertThat(foundSigner).isPresent();
		assertThat(foundSigner.orElseThrow().getId()).isEqualTo(signer.getId());
		assertThat(missingSigner).isEmpty();
	}

	@Test
	void shouldDeleteSignersByDocumentId() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		Document firstDocument = documentRepository.save(buildDocument(owner, "Contrato A"));
		Document secondDocument = documentRepository.save(buildDocument(owner, "Contrato B"));

		signerRepository.save(buildSigner(firstDocument, "Maria", "maria@example.com", "token-1"));
		signerRepository.save(buildSigner(firstDocument, "Joao", "joao@example.com", "token-2"));
		signerRepository.save(buildSigner(secondDocument, "Ana", "ana@example.com", "token-3"));
		signerRepository.flush();
		entityManager.clear();

		// Act
		signerRepository.deleteByDocumentId(firstDocument.getId());
		entityManager.flush();
		entityManager.clear();

		// Assert
		assertThat(signerRepository.findAllByDocumentId(firstDocument.getId())).isEmpty();
		assertThat(signerRepository.findAllByDocumentId(secondDocument.getId())).hasSize(1);
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

	private Signer buildSigner(Document document, String name, String email, String token) {
		Signer signer = new Signer();
		signer.setDocument(document);
		signer.setName(name);
		signer.setEmail(email);
		signer.setStatus(SignerStatus.PENDENTE);
		signer.setToken(token);
		signer.setTokenExpiresAt(Instant.parse("2026-04-25T10:00:00Z"));
		return signer;
	}
}
