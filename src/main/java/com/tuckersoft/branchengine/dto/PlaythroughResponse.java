package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.model.GameSession;

import java.time.Instant;

public record PlaythroughResponse(
        Long id,
        String playerTag,
        String ownerEmail,
        String startNodeCode,
        String currentNodeCode,
        Integer lucidity,
        Integer controlLevel,
        String status,
        String endingCode,
        Instant createdAt,
        Instant updatedAt
) {

    public static PlaythroughResponse from(GameSession session) {
        return new PlaythroughResponse(session.getId(), session.getPlayerTag(), session.getUser().getEmail(),
                session.getStartNodeCode(), session.getCurrentNode().getNodeCode(), session.getLucidity(),
                session.getControlLevel(), session.getStatus(), session.getEndingCode(),
                session.getCreatedAt(), session.getUpdatedAt());
    }
}
