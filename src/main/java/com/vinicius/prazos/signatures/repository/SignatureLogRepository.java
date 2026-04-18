package com.vinicius.prazos.signatures.repository;

import com.vinicius.prazos.signatures.domain.entity.SignatureLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignatureLogRepository extends JpaRepository<SignatureLog, Long> {

	void deleteByDocumentId(Long documentId);
}
