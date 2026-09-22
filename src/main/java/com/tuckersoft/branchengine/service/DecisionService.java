package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.model.*;
import com.tuckersoft.branchengine.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final GameSessionRepository gameSessionRepository;
    private final NodeRepository nodeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DecisionService(DecisionRepository decisionRepository,
                           RealityLogRepository realityLogRepository,
                           GameSessionRepository gameSessionRepository,
                           NodeRepository nodeRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.decisionRepository = decisionRepository;
        this.realityLogRepository = realityLogRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.nodeRepository = nodeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Decision processDecision(Long gameSessionId, String rawInput, boolean simulateHeader) {
        // 1. Validaciones
        GameSession session = gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new IllegalStateException("La sesión no está activa");
        }

        // 2. Lógica de clasificación
        String normalized = ClassificationEngine.normalize(rawInput);
        var result = ClassificationEngine.classify(rawInput);

        // 3. Crear y guardar decisión
        Decision decision = new Decision();
        decision.setGameSessionId(gameSessionId);
        decision.setRawInput(rawInput);
        decision.setNormalizedText(normalized);
        decision.setOutcomeCode(result.outcomeCode());
        decision.setHandlerUnit(result.handlerUnit());

        // 4. Actualización de stats (lucidity / controlLevel)
        int lucidityDelta = calculateLucidityDelta(result.outcomeCode());
        int controlDelta = calculateControlDelta(result.outcomeCode());

        int newLucidity = Math.max(0, Math.min(100, session.getLucidity() + lucidityDelta));
        int newControl = Math.max(0, Math.min(100, session.getControlLevel() + controlDelta));

        session.setLucidity(newLucidity);
        session.setControlLevel(newControl);

        // Registro de cambios
        RealityLog log = new RealityLog();
        log.getGameSessionId();
        log.setGameSessionId(gameSessionId);
        log.setLucidityDelta(lucidityDelta);
        log.setControlLevelDelta(controlDelta);
        log.setOutcomeCode(result.outcomeCode());
        realityLogRepository.save(log);

        // 5. Resolución de nodo destino
        Node currentNode = session.getCurrentNode();
        Node targetNode = nodeRepository.findBySourceNodeAndOutcomeCode(currentNode, result.outcomeCode())
                .orElse(currentNode); // Permanecer si no hay coincidencia

        decision.setTargetNodeId(targetNode.getId());
        session.setCurrentNode(targetNode);

        // 6. Resolución de estado final (Orden estricto de las 3 condiciones)
        if (newLucidity <= 0) {
            session.setStatus("INSANITY_ENDING");
        } else if (newControl <= 0) {
            session.setStatus("PAWN_ENDING");
        } else if (targetNode.isTerminal()) {
            session.setStatus("COMPLETED");
        }

        gameSessionRepository.save(session);
        Decision savedDecision = decisionRepository.save(decision);

        // 7. Publicación del evento asíncrono
        eventPublisher.publishEvent(new DecisionCommittedEvent(
                session.getId(),
                result.outcomeCode(),
                result.handlerUnit(),
                simulateHeader,
                session.getPlayerEmail()
        ));

        return savedDecision;
    }

    private int calculateLucidityDelta(String outcomeCode) {
        return switch (outcomeCode) {
            case "ACCEPTANCE" -> 5;
            case "REJECTION" -> -10;
            case "CONFLICT" -> -20;
            case "SUBMISSION" -> -5;
            default -> 0;
        };
    }

    private int calculateControlDelta(String outcomeCode) {
        return switch (outcomeCode) {
            case "ACCEPTANCE" -> 10;
            case "REJECTION" -> -5;
            case "CONFLICT" -> -15;
            case "SUBMISSION" -> 15;
            default -> 0;
        };
    }
}
