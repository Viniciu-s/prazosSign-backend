package com.vinicius.prazos.auth.security;

import com.vinicius.prazos.auth.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtProperties jwtProperties;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant expiration = now.plusMillis(jwtProperties.getExpiration());

		return Jwts.builder()
			.id(UUID.randomUUID().toString())
			.subject(user.getEmail())
			.claim("userId", user.getId())
			.claim("name", user.getName())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiration))
			.signWith(getSigningKey())
			.compact();
	}

	public String extractUsername(String token) {
		return extractAllClaims(token).getSubject();
	}

	public String extractTokenId(String token) {
		return extractAllClaims(token).getId();
	}

	public Instant extractExpiration(String token) {
		return extractAllClaims(token).getExpiration().toInstant();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return username.equalsIgnoreCase(userDetails.getUsername()) && extractExpiration(token).isAfter(Instant.now());
	}

	public long getExpirationInMillis() {
		return jwtProperties.getExpiration();
	}

	public boolean isTokenWellFormed(String token) {
		try {
			extractAllClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser()
			.verifyWith(getSigningKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}
}