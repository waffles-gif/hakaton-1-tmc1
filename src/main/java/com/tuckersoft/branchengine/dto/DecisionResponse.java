package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.model.Decision;

import java.time.Instant;

public record DecisionResponse(
        Long id,
        Long playthroughId,
        String playerTag,
        String sourceNodeCode,
        String resolvedNodeCode,
        String rawInput,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String status,
        String playthroughStatus,
        Integer lucidity,
        Integer controlLevel,
        String endingCode,
        Instant createdAt,
        Instant updatedAt
) {

    public static DecisionResponse from(Decision decision) {
        var playthrough = decision.getPlaythrough();
        return new DecisionResponse(
                decision.getId(),
                playthrough.getId(),
                playthrough.getPlayerTag(),
                decision.getNode().getNodeCode(),
                decision.getResolvedNodeCode(),
                decision.getRawInput(),
                decision.getBranchType(),
                decision.getImpactLevel(),
                decision.getHandlerUnit(),
                decision.getOutcomeCode(),
                decision.getStatus(),
                playthrough.getStatus(),
                playthrough.getLucidity(),
                playthrough.getControlLevel(),
                playthrough.getEndingCode(),
                decision.getCreatedAt(),
                decision.getUpdatedAt()
        );
    }
}
