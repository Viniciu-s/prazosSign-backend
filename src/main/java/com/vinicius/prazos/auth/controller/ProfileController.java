package com.vinicius.prazos.auth.controller;

import com.vinicius.prazos.auth.domain.dto.UserProfileResponse;
import com.vinicius.prazos.auth.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userProfileController")
@RequestMapping("/profile")
public class ProfileController {

	private final UserService userService;

	public ProfileController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public UserProfileResponse profile(@AuthenticationPrincipal UserDetails userDetails) {
		return userService.getProfileByEmail(userDetails.getUsername());
	}
}