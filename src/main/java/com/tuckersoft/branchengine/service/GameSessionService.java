package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.PlaythroughCreateRequest;
import com.tuckersoft.branchengine.dto.PlaythroughPathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.exception.BadRequestException;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.ForbiddenException;
import com.tuckersoft.branchengine.exception.NotFoundException;
import com.tuckersoft.branchengine.model.GameSession;
import com.tuckersoft.branchengine.model.Node;
import com.tuckersoft.branchengine.model.PlaythroughStatus;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.GameSessionRepository;
import com.tuckersoft.branchengine.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final UserRepository userRepository;
    private final NodeService nodeService;
    private final DecisionRepository decisionRepository;

    public GameSessionService(GameSessionRepository gameSessionRepository, UserRepository userRepository,
                               NodeService nodeService, DecisionRepository decisionRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.userRepository = userRepository;
        this.nodeService = nodeService;
        this.decisionRepository = decisionRepository;
    }

    @Transactional
    public PlaythroughResponse create(PlaythroughCreateRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        Node startNode = nodeService.findByNodeCodeOrThrow(request.startNodeCode());
        if (gameSessionRepository.existsByPlayerTag(request.playerTag())) {
            throw new ConflictException("Ya existe una partida con ese playerTag: " + request.playerTag());
        }
        if (startNode.getCurrentBranches() >= startNode.getBranchCapacity()) {
            throw new BadRequestException("El nodo está lleno: " + startNode.getNodeCode());
        }

        GameSession session = new GameSession();
        session.setPlayerTag(request.playerTag());
        session.setUser(owner);
        session.setStartNodeCode(startNode.getNodeCode());
        session.setCurrentNode(startNode);
        session.setLucidity(100);
        session.setControlLevel(0);
        session.setStatus(PlaythroughStatus.ACTIVA);
        session.setEndingCode(null);
        Instant now = Instant.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        startNode.setCurrentBranches(startNode.getCurrentBranches() + 1);
        nodeService.save(startNode);

        gameSessionRepository.save(session);
        return PlaythroughResponse.from(session);
    }

    @Transactional(readOnly = true)
    public List<PlaythroughResponse> listForUser(String requesterEmail, boolean isAdmin) {
        List<GameSession> sessions = isAdmin
                ? gameSessionRepository.findAllByOrderByCreatedAtDesc()
                : gameSessionRepository.findAllByUserEmailOrderByCreatedAtDesc(requesterEmail);
        return sessions.stream().map(PlaythroughResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlaythroughResponse getById(Long id, String requesterEmail, boolean isAdmin) {
        return PlaythroughResponse.from(findOwnedOrThrow(id, requesterEmail, isAdmin));
    }

    @Transactional(readOnly = true)
    public PlaythroughPathResponse getPath(Long id, String requesterEmail, boolean isAdmin) {
        GameSession session = findOwnedOrThrow(id, requesterEmail, isAdmin);
        var decisions = decisionRepository.findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(id);
        List<PlaythroughPathResponse.Step> steps = new java.util.ArrayList<>();
        int order = 1;
        for (var decision : decisions) {
            steps.add(new PlaythroughPathResponse.Step(order++, decision.getId(), decision.getNode().getNodeCode(),
                    decision.getResolvedNodeCode(), decision.getBranchType(), decision.getImpactLevel(),
                    decision.getCreatedAt()));
        }
        return new PlaythroughPathResponse(session.getId(), session.getPlayerTag(), session.getStatus(),
                session.getEndingCode(), session.getStartNodeCode(), session.getCurrentNode().getNodeCode(), steps);
    }

    private GameSession findOwnedOrThrow(Long id, String requesterEmail, boolean isAdmin) {
        GameSession session = gameSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Partida no encontrada: " + id));
        if (!isAdmin && !session.getUser().getEmail().equals(requesterEmail)) {
            throw new ForbiddenException("No puedes ver una partida ajena");
        }
        return session;
    }
}
