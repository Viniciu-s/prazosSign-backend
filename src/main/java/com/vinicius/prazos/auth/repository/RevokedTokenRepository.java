package com.vinicius.prazos.auth.repository;

import com.vinicius.prazos.auth.domain.entity.RevokedToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

	boolean existsByTokenId(String tokenId);

	void deleteByExpiresAtBefore(Instant expiresAt);
}