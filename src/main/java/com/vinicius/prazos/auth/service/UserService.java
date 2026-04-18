package com.vinicius.prazos.auth.service;

import com.vinicius.prazos.auth.domain.dto.UserProfileResponse;
import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getProfileByEmail(String email) {
		User user = userRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
		return toProfile(user);
	}

	public UserProfileResponse toProfile(User user) {
		return new UserProfileResponse(
			user.getId(),
			user.getName(),
			user.getEmail(),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}
}