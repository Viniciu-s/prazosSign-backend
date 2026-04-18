package com.vinicius.prazos.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinicius.prazos.auth.repository.PasswordResetTokenRepository;
import com.vinicius.prazos.auth.repository.RevokedTokenRepository;
import com.vinicius.prazos.auth.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Autowired
	private RevokedTokenRepository revokedTokenRepository;

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		passwordResetTokenRepository.deleteAll();
		revokedTokenRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void shouldRegisterLoginAccessProfileLogoutAndBlockToken() throws Exception {
		ApiResponse registerResponse = postJson("/auth/register", """
			{
			  "name": "Vinicius",
			  "email": "vinicius@example.com",
			  "password": "12345678"
			}
			""");

		assertThat(registerResponse.statusCode()).isEqualTo(201);
		assertThat(registerResponse.body()).isNotNull();
		assertThat(registerResponse.body().get("accessToken")).isInstanceOf(String.class);

		String registerToken = (String) registerResponse.body().get("accessToken");
		ApiResponse profileResponse = getWithBearer("/profile", registerToken);

		assertThat(profileResponse.statusCode()).isEqualTo(200);
		assertThat(profileResponse.body()).isNotNull();
		assertThat(profileResponse.body().get("name")).isEqualTo("Vinicius");
		assertThat(profileResponse.body().get("email")).isEqualTo("vinicius@example.com");

		ApiResponse loginResponse = postJson("/auth/login", """
			{
			  "email": "vinicius@example.com",
			  "password": "12345678"
			}
			""");

		assertThat(loginResponse.statusCode()).isEqualTo(200);
		assertThat(loginResponse.body()).isNotNull();
		String loginToken = (String) loginResponse.body().get("accessToken");

		ApiResponse logoutResponse = postWithBearer("/auth/logout", null, loginToken);
		assertThat(logoutResponse.statusCode()).isEqualTo(204);

		ApiResponse blockedProfileResponse = getWithBearer("/profile", loginToken);
		assertThat(blockedProfileResponse.statusCode()).isEqualTo(401);
	}

	@Test
	void shouldGeneratePasswordRecoveryTokenForExistingUser() throws Exception {
		postJson("/auth/register", """
			{
			  "name": "Maria",
			  "email": "maria@example.com",
			  "password": "12345678"
			}
			""");

		ApiResponse forgotPasswordResponse = postJson("/auth/forgot-password", """
			{
			  "email": "maria@example.com"
			}
			""");

		assertThat(forgotPasswordResponse.statusCode()).isEqualTo(200);
		assertThat(forgotPasswordResponse.body()).isNotNull();
		assertThat(forgotPasswordResponse.body().get("message")).isEqualTo("Token de recuperação gerado com sucesso.");
		assertThat(forgotPasswordResponse.body().get("resetToken")).isInstanceOf(String.class);
	}

	@Test
	void shouldReturnGenericMessageForUnknownEmailOnForgotPassword() throws Exception {
		ApiResponse forgotPasswordResponse = postJson("/auth/forgot-password", """
			{
			  "email": "unknown@example.com"
			}
			""");

		assertThat(forgotPasswordResponse.statusCode()).isEqualTo(200);
		assertThat(forgotPasswordResponse.body()).isNotNull();
		assertThat(forgotPasswordResponse.body().get("message")).isEqualTo("Se o e-mail existir, um token de recuperação será gerado.");
		assertThat(forgotPasswordResponse.body().get("resetToken")).isNull();
	}

	@Test
	void shouldResetPasswordAndAllowLoginOnlyWithNewPassword() throws Exception {
		postJson("/auth/register", """
			{
			  "name": "Joao",
			  "email": "joao@example.com",
			  "password": "12345678"
			}
			""");

		ApiResponse forgotPasswordResponse = postJson("/auth/forgot-password", """
			{
			  "email": "joao@example.com"
			}
			""");

		String resetToken = (String) forgotPasswordResponse.body().get("resetToken");
		ApiResponse resetPasswordResponse = postJson("/auth/reset-password", """
			{
			  "token": "%s",
			  "newPassword": "87654321"
			}
			""".formatted(resetToken));

		assertThat(resetPasswordResponse.statusCode()).isEqualTo(200);
		assertThat(resetPasswordResponse.body()).isNotNull();
		assertThat(resetPasswordResponse.body().get("message")).isEqualTo("Senha redefinida com sucesso.");

		ApiResponse loginWithOldPassword = postJson("/auth/login", """
			{
			  "email": "joao@example.com",
			  "password": "12345678"
			}
			""");
		assertThat(loginWithOldPassword.statusCode()).isEqualTo(401);
		assertThat(loginWithOldPassword.body()).isNotNull();
		assertThat(loginWithOldPassword.body().get("message")).isEqualTo("Credenciais inválidas");

		ApiResponse loginWithNewPassword = postJson("/auth/login", """
			{
			  "email": "joao@example.com",
			  "password": "87654321"
			}
			""");
		assertThat(loginWithNewPassword.statusCode()).isEqualTo(200);
		assertThat(loginWithNewPassword.body()).isNotNull();
		assertThat(loginWithNewPassword.body().get("accessToken")).isInstanceOf(String.class);
	}

	@Test
	void shouldRejectUsedResetToken() throws Exception {
		postJson("/auth/register", """
			{
			  "name": "Ana",
			  "email": "ana@example.com",
			  "password": "12345678"
			}
			""");

		ApiResponse forgotPasswordResponse = postJson("/auth/forgot-password", """
			{
			  "email": "ana@example.com"
			}
			""");

		String resetToken = (String) forgotPasswordResponse.body().get("resetToken");
		ApiResponse firstReset = postJson("/auth/reset-password", """
			{
			  "token": "%s",
			  "newPassword": "87654321"
			}
			""".formatted(resetToken));
		assertThat(firstReset.statusCode()).isEqualTo(200);

		ApiResponse secondReset = postJson("/auth/reset-password", """
			{
			  "token": "%s",
			  "newPassword": "99999999"
			}
			""".formatted(resetToken));

		assertThat(secondReset.statusCode()).isEqualTo(400);
		assertThat(secondReset.body()).isNotNull();
		assertThat(secondReset.body().get("message")).isEqualTo("Token de recuperação já utilizado");
	}

	private ApiResponse postJson(String path, String body) throws Exception {
		HttpRequest request = baseRequest(path)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		return send(request);
	}

	private ApiResponse postWithBearer(String path, String body, String token) throws Exception {
		HttpRequest.Builder builder = baseRequest(path)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		if (body == null) {
			builder.POST(HttpRequest.BodyPublishers.noBody());
		} else {
			builder
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(body));
		}

		return send(builder.build());
	}

	private ApiResponse getWithBearer(String path, String token) throws Exception {
		HttpRequest request = baseRequest(path)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.GET()
			.build();
		return send(request);
	}

	private HttpRequest.Builder baseRequest(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	private ApiResponse send(HttpRequest request) throws Exception {
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		Map<String, Object> body = response.body() == null || response.body().isBlank()
			? null
			: objectMapper.readValue(response.body(), new TypeReference<>() {});
		return new ApiResponse(response.statusCode(), body);
	}

	private record ApiResponse(int statusCode, Map<String, Object> body) {
	}
}