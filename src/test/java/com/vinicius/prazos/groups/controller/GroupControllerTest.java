package com.vinicius.prazos.groups.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vinicius.prazos.groups.domain.dto.GroupRequest;
import com.vinicius.prazos.groups.domain.dto.GroupDocumentResponse;
import com.vinicius.prazos.groups.domain.dto.GroupResponse;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.groups.service.GroupService;
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
class GroupControllerTest {

	@Mock
	private GroupService groupService;

	@InjectMocks
	private GroupController groupController;

	private UserDetails userDetails;

	@BeforeEach
	void setUp() {
		userDetails = User.withUsername("user@example.com")
			.password("password")
			.authorities(List.of())
			.build();
	}

	@Test
	void shouldListGroupsFromAuthenticatedUser() {
		// Arrange
		List<GroupResponse> expectedGroups = List.of(
			new GroupResponse(
				1L,
				"Grupo 1",
				Instant.parse("2026-04-18T09:00:00Z"),
				List.of(new GroupDocumentResponse(10L, "Contrato A", DocumentStatus.RASCUNHO, Instant.parse("2026-04-18T08:00:00Z"), Instant.parse("2026-04-18T09:00:00Z")))
			),
			new GroupResponse(2L, "Grupo 2", Instant.parse("2026-04-17T09:00:00Z"), List.of())
		);

		when(groupService.listGroups(userDetails.getUsername())).thenReturn(expectedGroups);

		// Act
		List<GroupResponse> response = groupController.list(userDetails);

		// Assert
		assertThat(response).isEqualTo(expectedGroups);
		verify(groupService).listGroups(userDetails.getUsername());
	}

	@Test
	void shouldCreateGroupForAuthenticatedUser() {
		// Arrange
		GroupRequest request = new GroupRequest("Novo Grupo");
		GroupResponse expectedResponse = new GroupResponse(1L, "Novo Grupo", Instant.parse("2026-04-18T09:00:00Z"), List.of());

		when(groupService.createGroup(userDetails.getUsername(), request)).thenReturn(expectedResponse);

		// Act
		GroupResponse response = groupController.create(userDetails, request);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(groupService).createGroup(userDetails.getUsername(), request);
	}

	@Test
	void shouldUpdateGroupForAuthenticatedUser() {
		// Arrange
		GroupRequest request = new GroupRequest("Atualizado");
		GroupResponse expectedResponse = new GroupResponse(1L, "Atualizado", Instant.parse("2026-04-18T09:00:00Z"), List.of());

		when(groupService.updateGroup(1L, userDetails.getUsername(), request)).thenReturn(expectedResponse);

		// Act
		GroupResponse response = groupController.update(1L, userDetails, request);

		// Assert
		assertThat(response).isEqualTo(expectedResponse);
		verify(groupService).updateGroup(1L, userDetails.getUsername(), request);
	}

	@Test
	void shouldDeleteGroupForAuthenticatedUser() {
		// Arrange

		// Act
		assertThatCode(() -> groupController.delete(1L, userDetails)).doesNotThrowAnyException();

		// Assert
		verify(groupService).deleteGroup(1L, userDetails.getUsername());
	}
}
