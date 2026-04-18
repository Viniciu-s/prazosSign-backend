package com.vinicius.prazos.signatures.repository;

import com.vinicius.prazos.signatures.domain.entity.Signer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignerRepository extends JpaRepository<Signer, Long> {

	List<Signer> findAllByDocumentId(Long documentId);

	boolean existsByDocumentIdAndEmailIgnoreCase(Long documentId, String email);

	Optional<Signer> findByToken(String token);

	void deleteByDocumentId(Long documentId);
}
