package com.vinicius.prazos.groups.service;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.groups.domain.dto.GroupRequest;
import com.vinicius.prazos.groups.domain.dto.GroupResponse;
import com.vinicius.prazos.groups.domain.entity.Group;
import com.vinicius.prazos.groups.repository.GroupRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

	private final GroupRepository groupRepository;
	private final CustomUserDetailsService userDetailsService;

	public GroupService(GroupRepository groupRepository, CustomUserDetailsService userDetailsService) {
		this.groupRepository = groupRepository;
		this.userDetailsService = userDetailsService;
	}

	@Transactional(readOnly = true)
	public List<GroupResponse> listGroups(String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		return groupRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
			.stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional
	public GroupResponse createGroup(String email, GroupRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);

		Group group = new Group();
		group.setUser(user);
		group.setName(normalizeName(request.name()));

		return toResponse(groupRepository.save(group));
	}

	@Transactional
	public GroupResponse updateGroup(Long id, String email, GroupRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Group group = loadOwnedGroup(id, user.getId());
		group.setName(normalizeName(request.name()));
		return toResponse(groupRepository.save(group));
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

	private GroupResponse toResponse(Group group) {
		return new GroupResponse(group.getId(), group.getName(), group.getCreatedAt());
	}
}