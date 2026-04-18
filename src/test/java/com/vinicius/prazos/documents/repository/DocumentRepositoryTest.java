package com.vinicius.prazos.documents.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.repository.UserRepository;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.groups.domain.entity.Group;
import com.vinicius.prazos.groups.repository.GroupRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DocumentRepositoryTest {

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		documentRepository.deleteAll();
		groupRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldFindAllByUserIdOrderedByUpdatedAtDesc() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		User anotherUser = userRepository.save(buildUser("another@example.com", "Another"));
		Group group = groupRepository.save(buildGroup(owner, "Financeiro"));

		Document olderDocument = documentRepository.save(buildDocument(owner, null, "Contrato Antigo", "Conteúdo", DocumentStatus.RASCUNHO));
		Document newerDocument = documentRepository.save(buildDocument(owner, group, "Contrato Novo", "Conteúdo", DocumentStatus.AGUARDANDO_ASSINATURA));
		documentRepository.save(buildDocument(anotherUser, null, "Documento de Outro Usuário", "Conteúdo", DocumentStatus.RASCUNHO));
		documentRepository.flush();

		updateUpdatedAt(olderDocument.getId(), Instant.parse("2026-04-17T09:00:00Z"));
		updateUpdatedAt(newerDocument.getId(), Instant.parse("2026-04-18T09:00:00Z"));
		entityManager.clear();

		// Act
		List<Document> documents = documentRepository.findAllByUserIdOrderByUpdatedAtDesc(owner.getId());

		// Assert
		assertThat(documents)
			.extracting(Document::getTitle)
			.containsExactly("Contrato Novo", "Contrato Antigo");
		assertThat(documents)
			.extracting(Document::getStatus)
			.containsExactly(DocumentStatus.AGUARDANDO_ASSINATURA, DocumentStatus.RASCUNHO);
	}

	@Test
	void shouldFindAllByUserIdAndStatusOrderedByUpdatedAtDesc() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));

		Document olderDraft = documentRepository.save(buildDocument(owner, null, "Rascunho Antigo", "Conteúdo", DocumentStatus.RASCUNHO));
		Document newerDraft = documentRepository.save(buildDocument(owner, null, "Rascunho Novo", "Conteúdo", DocumentStatus.RASCUNHO));
		documentRepository.save(buildDocument(owner, null, "Documento Enviado", "Conteúdo", DocumentStatus.AGUARDANDO_ASSINATURA));
		documentRepository.flush();

		updateUpdatedAt(olderDraft.getId(), Instant.parse("2026-04-17T09:00:00Z"));
		updateUpdatedAt(newerDraft.getId(), Instant.parse("2026-04-18T09:00:00Z"));
		entityManager.clear();

		// Act
		List<Document> documents = documentRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(owner.getId(), DocumentStatus.RASCUNHO);

		// Assert
		assertThat(documents)
			.extracting(Document::getTitle)
			.containsExactly("Rascunho Novo", "Rascunho Antigo");
		assertThat(documents)
			.extracting(Document::getStatus)
			.containsOnly(DocumentStatus.RASCUNHO);
	}

	@Test
	void shouldFindDocumentByIdAndUserId() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		User anotherUser = userRepository.save(buildUser("another@example.com", "Another"));
		Document document = documentRepository.saveAndFlush(buildDocument(owner, null, "Contrato", "Conteúdo", DocumentStatus.RASCUNHO));
		entityManager.clear();

		// Act
		Optional<Document> foundDocument = documentRepository.findByIdAndUserId(document.getId(), owner.getId());
		Optional<Document> missingDocument = documentRepository.findByIdAndUserId(document.getId(), anotherUser.getId());

		// Assert
		assertThat(foundDocument).isPresent();
		assertThat(foundDocument.orElseThrow().getTitle()).isEqualTo("Contrato");
		assertThat(missingDocument).isEmpty();
	}

	private User buildUser(String email, String name) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		return user;
	}

	private Group buildGroup(User user, String name) {
		Group group = new Group();
		group.setUser(user);
		group.setName(name);
		return group;
	}

	private Document buildDocument(User user, Group group, String title, String content, DocumentStatus status) {
		Document document = new Document();
		document.setUser(user);
		document.setGroup(group);
		document.setTitle(title);
		document.setContent(content);
		document.setStatus(status);
		return document;
	}

	private void updateUpdatedAt(Long documentId, Instant updatedAt) {
		jdbcTemplate.update(
			"update documents set updated_at = ? where id = ?",
			Timestamp.from(updatedAt),
			documentId
		);
	}
}
