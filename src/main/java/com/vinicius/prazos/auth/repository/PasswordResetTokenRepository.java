package com.vinicius.prazos.auth.repository;

import com.vinicius.prazos.auth.domain.entity.PasswordResetToken;
import com.vinicius.prazos.auth.domain.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	void deleteByUser(User user);

	Optional<PasswordResetToken> findByToken(String token);
}