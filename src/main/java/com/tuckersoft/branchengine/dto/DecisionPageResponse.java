package com.tuckersoft.branchengine.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record DecisionPageResponse(
        List<DecisionResponse> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {

    public static DecisionPageResponse from(Page<com.tuckersoft.branchengine.model.Decision> page) {
        return new DecisionPageResponse(
                page.getContent().stream().map(DecisionResponse::from).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
