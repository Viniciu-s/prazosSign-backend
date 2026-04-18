package com.vinicius.prazos.groups.domain.dto;

import java.time.Instant;

public record GroupResponse(Long id, String name, Instant createdAt) {
}