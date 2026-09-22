package com.tuckersoft.branchengine.event;

public record DecisionCommittedEvent(
        Long gameSessionId,
        String outcomeCode,
        String handlerUnit,
        boolean simulateHeaderPresent,
        String playerEmail
) {}