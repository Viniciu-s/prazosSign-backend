package com.vinicius.prazos.groups.repository;

import com.vinicius.prazos.groups.domain.entity.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {

	List<Group> findAllByUserIdOrderByCreatedAtDesc(Long userId);

	Optional<Group> findByIdAndUserId(Long id, Long userId);
}