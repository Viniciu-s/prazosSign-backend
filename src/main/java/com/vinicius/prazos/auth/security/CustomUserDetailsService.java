package com.vinicius.prazos.auth.security;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		User user = loadDomainUserByEmail(username);
		return org.springframework.security.core.userdetails.User
			.withUsername(user.getEmail())
			.password(user.getPasswordHash())
			.authorities(List.of())
			.build();
	}

	public User loadDomainUserByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
	}
}