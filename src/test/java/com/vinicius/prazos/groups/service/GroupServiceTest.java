package com.vinicius.prazos.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.groups.domain.dto.GroupRequest;
import com.vinicius.prazos.groups.domain.dto.GroupResponse;
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
class GroupServiceTest {

	@Mock
	private GroupRepository groupRepository;

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private CustomUserDetailsService userDetailsService;

	@InjectMocks
	private GroupService groupService;

	private User user;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(1L);
		user.setEmail("user@example.com");
		user.setName("User");
	}

	@Test
	void shouldListGroups() {
		// Arrange
		Group firstGroup = buildGroup(1L, "Grupo 1", Instant.parse("2026-04-17T10:15:30Z"));
		Group secondGroup = buildGroup(2L, "Grupo 2", Instant.parse("2026-04-16T09:00:00Z"));
		Document firstDocument = buildDocument(10L, firstGroup, "Contrato A", DocumentStatus.AGUARDANDO_ASSINATURA, Instant.parse("2026-04-18T10:00:00Z"), Instant.parse("2026-04-18T11:00:00Z"));
		Document secondDocument = buildDocument(11L, firstGroup, "Contrato B", DocumentStatus.RASCUNHO, Instant.parse("2026-04-17T10:00:00Z"), Instant.parse("2026-04-18T09:00:00Z"));

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(groupRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(firstGroup, secondGroup));
		when(documentRepository.findAllByUserIdAndGroupIdInOrderByUpdatedAtDesc(user.getId(), List.of(1L, 2L)))
			.thenReturn(List.of(firstDocument, secondDocument));

		// Act
		List<GroupResponse> response = groupService.listGroups(user.getEmail());

		// Assert
		assertThat(response)
			.extracting(GroupResponse::name)
			.containsExactly("Grupo 1", "Grupo 2");
		assertThat(response)
			.extracting(GroupResponse::id)
			.containsExactly(1L, 2L);
		assertThat(response.getFirst().documents())
			.extracting(document -> document.title())
			.containsExactly("Contrato A", "Contrato B");
		assertThat(response.get(1).documents()).isEmpty();
	}

	@Test
	void shouldCreateGroupWithTrimmedName() {
		// Arrange
		GroupRequest request = new GroupRequest("  Novo Grupo  ");
		ArgumentCaptor<Group> savedGroupCaptor = ArgumentCaptor.forClass(Group.class);

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
			Group group = invocation.getArgument(0);
			group.setId(10L);
			group.setCreatedAt(Instant.parse("2026-04-18T12:00:00Z"));
			return group;
		});

		// Act
		GroupResponse response = groupService.createGroup(user.getEmail(), request);

		// Assert
		verify(groupRepository).save(savedGroupCaptor.capture());
		assertThat(savedGroupCaptor.getValue().getUser()).isSameAs(user);
		assertThat(savedGroupCaptor.getValue().getName()).isEqualTo("Novo Grupo");
		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.name()).isEqualTo("Novo Grupo");
		assertThat(response.documents()).isEmpty();
	}

	@Test
	void shouldRejectBlankGroupNameOnCreate() {
		// Arrange
		GroupRequest request = new GroupRequest("   ");

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);

		// Act / Assert
		assertThatThrownBy(() -> groupService.createGroup(user.getEmail(), request))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(exception.getReason()).isEqualTo("Nome do grupo é obrigatório");
			});
	}

	@Test
	void shouldUpdateOwnedGroup() {
		// Arrange
		Group existingGroup = buildGroup(1L, "Antigo", Instant.parse("2026-04-17T10:15:30Z"));
		existingGroup.setUser(user);

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(groupRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(existingGroup));
		when(groupRepository.save(existingGroup)).thenReturn(existingGroup);

		// Act
		GroupResponse response = groupService.updateGroup(1L, user.getEmail(), new GroupRequest("  Atualizado  "));

		// Assert
		assertThat(existingGroup.getName()).isEqualTo("Atualizado");
		assertThat(response.name()).isEqualTo("Atualizado");
		assertThat(response.documents()).isEmpty();
		verify(groupRepository).save(existingGroup);
	}

	@Test
	void shouldDeleteOwnedGroup() {
		// Arrange
		Group existingGroup = buildGroup(1L, "Grupo", Instant.parse("2026-04-17T10:15:30Z"));
		existingGroup.setUser(user);

		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(groupRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(existingGroup));

		// Act
		groupService.deleteGroup(1L, user.getEmail());

		// Assert
		verify(groupRepository).delete(existingGroup);
	}

	@Test
	void shouldThrowWhenUpdatingMissingGroup() {
		// Arrange
		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(groupRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

		// Act / Assert
		assertThatThrownBy(() -> groupService.updateGroup(1L, user.getEmail(), new GroupRequest("Atualizado")))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(exception.getReason()).isEqualTo("Grupo não encontrado");
			});
	}

	@Test
	void shouldThrowWhenDeletingMissingGroup() {
		// Arrange
		when(userDetailsService.loadDomainUserByEmail(user.getEmail())).thenReturn(user);
		when(groupRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

		// Act / Assert
		assertThatThrownBy(() -> groupService.deleteGroup(1L, user.getEmail()))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(exception.getReason()).isEqualTo("Grupo não encontrado");
			});
	}

	private Group buildGroup(Long id, String name, Instant createdAt) {
		Group group = new Group();
		group.setId(id);
		group.setName(name);
		group.setCreatedAt(createdAt);
		return group;
	}

	private Document buildDocument(
		Long id,
		Group group,
		String title,
		DocumentStatus status,
		Instant createdAt,
		Instant updatedAt
	) {
		Document document = new Document();
		document.setId(id);
		document.setGroup(group);
		document.setTitle(title);
		document.setStatus(status);
		document.setCreatedAt(createdAt);
		document.setUpdatedAt(updatedAt);
		return document;
	}
}
