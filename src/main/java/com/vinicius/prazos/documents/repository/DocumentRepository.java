package com.vinicius.prazos.documents.repository;

import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	List<Document> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

	List<Document> findAllByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, DocumentStatus status);

	List<Document> findAllByUserIdAndGroupIdInOrderByUpdatedAtDesc(Long userId, List<Long> groupIds);

	Optional<Document> findByIdAndUserId(Long id, Long userId);
}
