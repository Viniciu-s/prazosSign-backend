package com.vinicius.prazos.groups.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.repository.UserRepository;
import com.vinicius.prazos.groups.domain.entity.Group;
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
class GroupRepositoryTest {

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
		groupRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldFindAllByUserIdOrderedByCreatedAtDesc() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		User anotherUser = userRepository.save(buildUser("another@example.com", "Another"));

		Group olderGroup = groupRepository.save(buildGroup(owner, "Grupo Antigo"));
		Group newerGroup = groupRepository.save(buildGroup(owner, "Grupo Novo"));
		groupRepository.save(buildGroup(anotherUser, "Grupo de Outro Usuario"));
		groupRepository.flush();

		updateCreatedAt(olderGroup.getId(), Instant.parse("2026-04-15T09:00:00Z"));
		updateCreatedAt(newerGroup.getId(), Instant.parse("2026-04-18T09:00:00Z"));
		entityManager.clear();

		// Act
		List<Group> groups = groupRepository.findAllByUserIdOrderByCreatedAtDesc(owner.getId());

		// Assert
		assertThat(groups)
			.extracting(Group::getName)
			.containsExactly("Grupo Novo", "Grupo Antigo");
		assertThat(groups)
			.extracting(group -> group.getUser().getId())
			.containsOnly(owner.getId());
	}

	@Test
	void shouldFindGroupByIdAndUserId() {
		// Arrange
		User owner = userRepository.save(buildUser("owner@example.com", "Owner"));
		User anotherUser = userRepository.save(buildUser("another@example.com", "Another"));
		Group group = groupRepository.saveAndFlush(buildGroup(owner, "Grupo"));
		entityManager.clear();

		// Act
		Optional<Group> foundGroup = groupRepository.findByIdAndUserId(group.getId(), owner.getId());
		Optional<Group> missingGroup = groupRepository.findByIdAndUserId(group.getId(), anotherUser.getId());

		// Assert
		assertThat(foundGroup).isPresent();
		assertThat(foundGroup.orElseThrow().getName()).isEqualTo("Grupo");
		assertThat(missingGroup).isEmpty();
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

	private void updateCreatedAt(Long groupId, Instant createdAt) {
		jdbcTemplate.update(
			"update groups set created_at = ? where id = ?",
			Timestamp.from(createdAt),
			groupId
		);
	}
}
