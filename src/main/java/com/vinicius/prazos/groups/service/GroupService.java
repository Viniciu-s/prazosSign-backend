package com.vinicius.prazos.groups.service;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.groups.domain.dto.GroupDocumentResponse;
import com.vinicius.prazos.groups.domain.dto.GroupRequest;
import com.vinicius.prazos.groups.domain.dto.GroupResponse;
import com.vinicius.prazos.groups.domain.entity.Group;
import com.vinicius.prazos.groups.repository.GroupRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

	private final GroupRepository groupRepository;
	private final DocumentRepository documentRepository;
	private final CustomUserDetailsService userDetailsService;

	public GroupService(
		GroupRepository groupRepository,
		DocumentRepository documentRepository,
		CustomUserDetailsService userDetailsService
	) {
		this.groupRepository = groupRepository;
		this.documentRepository = documentRepository;
		this.userDetailsService = userDetailsService;
	}

	@Transactional(readOnly = true)
	public List<GroupResponse> listGroups(String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		List<Group> groups = groupRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
		Map<Long, List<GroupDocumentResponse>> documentsByGroupId = loadDocumentsByGroupId(user.getId(), groups);
		return groups
			.stream()
			.map(group -> toResponse(group, documentsByGroupId.getOrDefault(group.getId(), List.of())))
			.toList();
	}

	@Transactional
	public GroupResponse createGroup(String email, GroupRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);

		Group group = new Group();
		group.setUser(user);
		group.setName(normalizeName(request.name()));

		return toResponse(groupRepository.save(group), List.of());
	}

	@Transactional
	public GroupResponse updateGroup(Long id, String email, GroupRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Group group = loadOwnedGroup(id, user.getId());
		group.setName(normalizeName(request.name()));
		return toResponse(groupRepository.save(group), List.of());
	}

	@Transactional
	public void deleteGroup(Long id, String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Group group = loadOwnedGroup(id, user.getId());
		groupRepository.delete(group);
	}

	private Group loadOwnedGroup(Long id, Long userId) {
		return groupRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo não encontrado"));
	}

	private String normalizeName(String name) {
		String normalizedName = name.trim();
		if (normalizedName.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do grupo é obrigatório");
		}
		return normalizedName;
	}

	private Map<Long, List<GroupDocumentResponse>> loadDocumentsByGroupId(Long userId, List<Group> groups) {
		if (groups.isEmpty()) {
			return Map.of();
		}

		List<Long> groupIds = groups.stream()
			.map(Group::getId)
			.toList();

		return documentRepository.findAllByUserIdAndGroupIdInOrderByUpdatedAtDesc(userId, groupIds)
			.stream()
			.collect(Collectors.groupingBy(
				document -> document.getGroup().getId(),
				Collectors.mapping(this::toGroupDocumentResponse, Collectors.toList())
			));
	}

	private GroupDocumentResponse toGroupDocumentResponse(Document document) {
		return new GroupDocumentResponse(
			document.getId(),
			document.getTitle(),
			document.getStatus(),
			document.getCreatedAt(),
			document.getUpdatedAt()
		);
	}

	private GroupResponse toResponse(Group group, List<GroupDocumentResponse> documents) {
		return new GroupResponse(group.getId(), group.getName(), group.getCreatedAt(), documents);
	}
}
