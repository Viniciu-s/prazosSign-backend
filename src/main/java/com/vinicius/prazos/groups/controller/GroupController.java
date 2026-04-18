package com.vinicius.prazos.groups.controller;

import com.vinicius.prazos.groups.domain.dto.GroupRequest;
import com.vinicius.prazos.groups.domain.dto.GroupResponse;
import com.vinicius.prazos.groups.service.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups")
public class GroupController {

	private final GroupService groupService;

	public GroupController(GroupService groupService) {
		this.groupService = groupService;
	}

	@GetMapping
	public List<GroupResponse> list(@AuthenticationPrincipal UserDetails userDetails) {
		return groupService.listGroups(userDetails.getUsername());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public GroupResponse create(
		@AuthenticationPrincipal UserDetails userDetails,
		@Valid @RequestBody GroupRequest request
	) {
		return groupService.createGroup(userDetails.getUsername(), request);
	}

	@PutMapping("/{id}")
	public GroupResponse update(
		@PathVariable Long id,
		@AuthenticationPrincipal UserDetails userDetails,
		@Valid @RequestBody GroupRequest request
	) {
		return groupService.updateGroup(id, userDetails.getUsername(), request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
		groupService.deleteGroup(id, userDetails.getUsername());
	}
}