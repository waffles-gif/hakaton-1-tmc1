package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.ForbiddenException;
import com.tuckersoft.branchengine.exception.NotFoundException;
import com.tuckersoft.branchengine.model.BranchType;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.DecisionStatus;
import com.tuckersoft.branchengine.model.EndingCode;
import com.tuckersoft.branchengine.model.GameSession;
import com.tuckersoft.branchengine.model.ImpactLevel;
import com.tuckersoft.branchengine.model.Node;
import com.tuckersoft.branchengine.model.PlaythroughStatus;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.GameSessionRepository;
import com.tuckersoft.branchengine.repository.NodeRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final GameSessionRepository gameSessionRepository;
    private final NodeRepository nodeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DecisionService(DecisionRepository decisionRepository,
                           GameSessionRepository gameSessionRepository,
                           NodeRepository nodeRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.decisionRepository = decisionRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.nodeRepository = nodeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Decision createDecision(Long playthroughId, String rawInput, String impactLevel,
                                   String ownerEmail, boolean simulateFailure) {
        // 1. La partida es del usuario autenticado
        GameSession playthrough = gameSessionRepository.findById(playthroughId)
                .orElseThrow(() -> new NotFoundException("Partida no encontrada: " + playthroughId));
        if (!playthrough.getUser().getEmail().equals(ownerEmail)) {
            throw new ForbiddenException("No puedes decidir sobre una partida ajena");
        }
        if (PlaythroughStatus.FINALIZADA.equals(playthrough.getStatus())) {
            throw new ConflictException("La partida ya está finalizada");
        }

        // 2. Clasificar y derivar handlerUnit / outcomeCode
        ClassificationEngine.ClassificationResult result = ClassificationEngine.classify(rawInput);
        Node originNode = playthrough.getCurrentNode();
        Instant now = Instant.now();

        Decision decision = new Decision();
        decision.setPlaythrough(playthrough);
        decision.setNode(originNode);
        decision.setRawInput(rawInput);
        decision.setBranchType(result.branchType());
        decision.setImpactLevel(impactLevel);
        decision.setHandlerUnit(result.handlerUnit());
        decision.setOutcomeCode(result.outcomeCode());
        decision.setCreatedAt(now);
        decision.setUpdatedAt(now);

        // 3. Entrada corrupta: se guarda con ERROR, no se toca la partida ni se publica el evento
        if (BranchType.ENTRADA_CORRUPTA.equals(result.branchType())) {
            decision.setResolvedNodeCode(null);
            decision.setStatus(DecisionStatus.ERROR);
            return decisionRepository.save(decision);
        }

        // 4. Aplicar stats según el impacto, con límites [0, 100]
        int[] deltas = statDeltas(impactLevel);
        int newLucidity = clamp(playthrough.getLucidity() + deltas[0]);
        int newControl = clamp(playthrough.getControlLevel() + deltas[1]);

        // 5. Resolver el nodo destino
        String resolvedNodeCode = (BranchType.RUPTURA_CUARTA_PARED.equals(result.branchType())
                || ImpactLevel.CRITICO.equals(impactLevel))
                ? originNode.getGlitchBranchCode()
                : originNode.getPrimaryBranchCode();

        // 6. Resolver el estado de la partida, en este orden exacto
        Node targetNode = resolvedNodeCode == null ? null : nodeRepository.findByNodeCode(resolvedNodeCode).orElse(null);

        playthrough.setLucidity(newLucidity);
        playthrough.setControlLevel(newControl);
        playthrough.setUpdatedAt(now);

        if (newControl >= 100) {
            playthrough.setStatus(PlaythroughStatus.FINALIZADA);
            playthrough.setEndingCode(EndingCode.ENDING_PAC_SYMBOL);
        } else if (newLucidity <= 0) {
            playthrough.setStatus(PlaythroughStatus.FINALIZADA);
            playthrough.setEndingCode(EndingCode.ENDING_WHITE_BEAR);
        } else if (targetNode == null) {
            playthrough.setStatus(PlaythroughStatus.FINALIZADA);
            playthrough.setEndingCode(EndingCode.ENDING_NETFLIX_CUT);
        } else {
            playthrough.setStatus(PlaythroughStatus.ACTIVA);
            playthrough.setCurrentNode(targetNode);
        }

        decision.setResolvedNodeCode(resolvedNodeCode);
        decision.setStatus(DecisionStatus.REGISTRADA);

        gameSessionRepository.save(playthrough);
        Decision savedDecision = decisionRepository.save(decision);

        // 7. Publicar el evento asíncrono (destinatario: el email del dueño de la partida)
        eventPublisher.publishEvent(new DecisionCommittedEvent(
                savedDecision.getId(),
                playthrough.getUser().getEmail(),
                playthrough.getUser().getDisplayName(),
                playthrough.getPlayerTag(),
                savedDecision.getBranchType(),
                savedDecision.getImpactLevel(),
                savedDecision.getHandlerUnit(),
                savedDecision.getOutcomeCode(),
                originNode.getNodeCode(),
                savedDecision.getResolvedNodeCode(),
                savedDecision.getRawInput(),
                playthrough.getStatus(),
                playthrough.getLucidity(),
                playthrough.getControlLevel(),
                playthrough.getEndingCode(),
                savedDecision.getCreatedAt(),
                simulateFailure
        ));

        return savedDecision;
    }

    @Transactional(readOnly = true)
    public Decision getOwnedOrThrow(Long id, String requesterEmail, boolean isAdmin) {
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Decisión no encontrada: " + id));
        if (!isAdmin && !decision.getPlaythrough().getUser().getEmail().equals(requesterEmail)) {
            throw new ForbiddenException("No puedes ver una decisión ajena");
        }
        return decision;
    }

    @Transactional(readOnly = true)
    public Page<Decision> search(String requesterEmail, boolean isAdmin, String branchType, String impactLevel,
                                 String status, Long playthroughId, Pageable pageable) {
        Specification<Decision> spec = (root, query, cb) -> cb.conjunction();
        if (!isAdmin) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("playthrough").get("user").get("email"), requesterEmail));
        }
        if (branchType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("branchType"), branchType));
        }
        if (impactLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("impactLevel"), impactLevel));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (playthroughId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("playthrough").get("id"), playthroughId));
        }
        return decisionRepository.findAll(spec, pageable);
    }

    private int[] statDeltas(String impactLevel) {
        return switch (impactLevel) {
            case ImpactLevel.LEVE -> new int[]{-5, 5};
            case ImpactLevel.MODERADO -> new int[]{-15, 10};
            case ImpactLevel.GRAVE -> new int[]{-30, 20};
            case ImpactLevel.CRITICO -> new int[]{-40, 45};
            default -> new int[]{0, 0};
        };
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
