package com.vinicius.prazos.groups.domain.dto;

import java.time.Instant;
import java.util.List;

public record GroupResponse(Long id, String name, Instant createdAt, List<GroupDocumentResponse> documents) {
}
