package com.vinicius.prazos.auth.service;

import com.vinicius.prazos.auth.domain.dto.AuthResponse;
import com.vinicius.prazos.auth.domain.dto.ForgotPasswordRequest;
import com.vinicius.prazos.auth.domain.dto.ForgotPasswordResponse;
import com.vinicius.prazos.auth.domain.dto.LoginRequest;
import com.vinicius.prazos.auth.domain.dto.RegisterRequest;
import com.vinicius.prazos.auth.domain.dto.ResetPasswordRequest;
import com.vinicius.prazos.auth.domain.dto.ResetPasswordResponse;
import com.vinicius.prazos.auth.domain.entity.PasswordResetToken;
import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.repository.PasswordResetTokenRepository;
import com.vinicius.prazos.auth.repository.UserRepository;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.auth.security.JwtService;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final CustomUserDetailsService userDetailsService;
	private final UserService userService;
	private final TokenBlacklistService tokenBlacklistService;

	public AuthService(
		UserRepository userRepository,
		PasswordResetTokenRepository passwordResetTokenRepository,
		AuthenticationManager authenticationManager,
		PasswordEncoder passwordEncoder,
		JwtService jwtService,
		CustomUserDetailsService userDetailsService,
		UserService userService,
		TokenBlacklistService tokenBlacklistService
	) {
		this.userRepository = userRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.authenticationManager = authenticationManager;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.userService = userService;
		this.tokenBlacklistService = tokenBlacklistService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
		}

		User user = new User();
		user.setName(request.name().trim());
		user.setEmail(normalizedEmail);
		user.setPasswordHash(passwordEncoder.encode(request.password()));

		User savedUser = userRepository.save(user);
		return buildAuthResponse(savedUser);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		try {
			authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
			);
		} catch (BadCredentialsException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
		}

		User user = userDetailsService.loadDomainUserByEmail(normalizedEmail);
		return buildAuthResponse(user);
	}

	@Transactional
	public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

		if (user == null) {
			return new ForgotPasswordResponse(
				"Se o e-mail existir, um token de recuperação será gerado.",
				null
			);
		}

		passwordResetTokenRepository.deleteByUser(user);

		PasswordResetToken passwordResetToken = new PasswordResetToken();
		passwordResetToken.setUser(user);
		passwordResetToken.setToken(UUID.randomUUID().toString().replace("-", ""));
		passwordResetToken.setExpiresAt(Instant.now().plusSeconds(30 * 60));
		passwordResetTokenRepository.save(passwordResetToken);

		return new ForgotPasswordResponse(
			"Token de recuperação gerado com sucesso.",
			passwordResetToken.getToken()
		);
	}

	@Transactional
	public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
		PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(request.token().trim())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperação inválido"));

		if (passwordResetToken.getUsedAt() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperação já utilizado");
		}

		if (passwordResetToken.getExpiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperação expirado");
		}

		User user = passwordResetToken.getUser();
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		passwordResetToken.setUsedAt(Instant.now());

		userRepository.save(user);
		passwordResetTokenRepository.save(passwordResetToken);

		return new ResetPasswordResponse("Senha redefinida com sucesso.");
	}

	@Transactional
	public void logout(String token) {
		if (!jwtService.isTokenWellFormed(token)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
		}

		tokenBlacklistService.revoke(jwtService.extractTokenId(token), jwtService.extractExpiration(token));
	}

	private AuthResponse buildAuthResponse(User user) {
		String accessToken = jwtService.generateToken(user);
		return new AuthResponse(
			accessToken,
			"Bearer",
			jwtService.getExpirationInMillis(),
			userService.toProfile(user)
		);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}