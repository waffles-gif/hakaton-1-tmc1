package com.tuckersoft.branchengine.event;

import java.time.Instant;

/**
 * Todo lo que el listener necesita ya viaja aquí: corre en otro hilo, después del commit,
 * y ahí ya no hay usuario autenticado ni sesión de Hibernate abierta.
 */
public record DecisionCommittedEvent(
        Long decisionId,
        String recipientEmail,
        String recipientDisplayName,
        String playerTag,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String sourceNodeCode,
        String resolvedNodeCode,
        String rawInput,
        String playthroughStatus,
        Integer lucidity,
        Integer controlLevel,
        String endingCode,
        Instant decisionCreatedAt,
        boolean simulateFailure
) {
}
