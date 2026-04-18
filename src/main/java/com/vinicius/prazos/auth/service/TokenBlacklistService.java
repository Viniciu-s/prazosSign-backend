package com.vinicius.prazos.auth.service;

import com.vinicius.prazos.auth.domain.entity.RevokedToken;
import com.vinicius.prazos.auth.repository.RevokedTokenRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenBlacklistService {

	private final RevokedTokenRepository revokedTokenRepository;

	public TokenBlacklistService(RevokedTokenRepository revokedTokenRepository) {
		this.revokedTokenRepository = revokedTokenRepository;
	}

	@Transactional
	public void revoke(String tokenId, Instant expiresAt) {
		cleanupExpired();
		if (revokedTokenRepository.existsByTokenId(tokenId)) {
			return;
		}

		RevokedToken revokedToken = new RevokedToken();
		revokedToken.setTokenId(tokenId);
		revokedToken.setExpiresAt(expiresAt);
		revokedTokenRepository.save(revokedToken);
	}

	@Transactional(readOnly = true)
	public boolean isRevoked(String tokenId) {
		return revokedTokenRepository.existsByTokenId(tokenId);
	}

	@Transactional
	public void cleanupExpired() {
		revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
	}
}