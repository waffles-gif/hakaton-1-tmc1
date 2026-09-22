package com.tuckersoft.service;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.model.BranchType;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.DecisionStatus;
import com.tuckersoft.branchengine.model.EndingCode;
import com.tuckersoft.branchengine.model.GameSession;
import com.tuckersoft.branchengine.model.ImpactLevel;
import com.tuckersoft.branchengine.model.Node;
import com.tuckersoft.branchengine.model.PlaythroughStatus;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.GameSessionRepository;
import com.tuckersoft.branchengine.repository.NodeRepository;
import com.tuckersoft.branchengine.service.ClassificationEngine;
import com.tuckersoft.branchengine.service.DecisionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    private static final String OWNER_EMAIL = "player@tuckersoft.com";

    @Mock private DecisionRepository decisionRepository;
    @Mock private GameSessionRepository gameSessionRepository;
    @Mock private NodeRepository nodeRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DecisionService decisionService;

    private GameSession playthrough;
    private Node currentNode;

    @BeforeEach
    void setUp() {
        decisionService = new DecisionService(decisionRepository, gameSessionRepository, nodeRepository, eventPublisher);

        currentNode = new Node();
        currentNode.setId(10L);
        currentNode.setNodeCode("NODE-CEREAL");
        currentNode.setPrimaryBranchCode("NODE-BUS");
        currentNode.setGlitchBranchCode("NODE-ESPEJO");

        User owner = new User();
        owner.setEmail(OWNER_EMAIL);
        owner.setDisplayName("Ada Lovelace");

        playthrough = new GameSession();
        playthrough.setId(1L);
        playthrough.setPlayerTag("STEFAN-01");
        playthrough.setUser(owner);
        playthrough.setStatus(PlaythroughStatus.ACTIVA);
        playthrough.setLucidity(50);
        playthrough.setControlLevel(50);
        playthrough.setCurrentNode(currentNode);

        org.mockito.Mockito.lenient().when(decisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("1. 'Stefan destruye la camara' clasifica como RUPTURA_CUARTA_PARED (precedencia)")
    void precedenciaDeReglasDaRupturaCuartaPared() {
        var resultado = ClassificationEngine.classify("Stefan destruye la camara");
        assertEquals(BranchType.RUPTURA_CUARTA_PARED, resultado.branchType());
    }

    @Test
    @DisplayName("2. Un texto sin letras es ENTRADA_CORRUPTA y no modifica la partida")
    void entradaSinLetrasEsCorruptaYNoTocaLaPartida() {
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(playthrough));

        Decision decision = decisionService.createDecision(1L, "1984 !!! 2001", ImpactLevel.LEVE, OWNER_EMAIL, false);

        assertEquals(BranchType.ENTRADA_CORRUPTA, decision.getBranchType());
        assertEquals(DecisionStatus.ERROR, decision.getStatus());
        assertNull(decision.getResolvedNodeCode());
        assertEquals(50, playthrough.getLucidity());
        assertEquals(50, playthrough.getControlLevel());
        assertEquals(PlaythroughStatus.ACTIVA, playthrough.getStatus());
    }

    @Test
    @DisplayName("3. Impacto CRITICO baja lucidity 40 y sube controlLevel 45, sin pasarse de los límites")
    void impactoCriticoAplicaLosDeltasCorrectos() {
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(playthrough));
        when(nodeRepository.findByNodeCode("NODE-ESPEJO")).thenReturn(Optional.of(currentNode));

        decisionService.createDecision(1L, "Stefan acepta la oferta de Mohan", ImpactLevel.CRITICO, OWNER_EMAIL, false);

        assertEquals(10, playthrough.getLucidity());
        assertEquals(95, playthrough.getControlLevel());
    }

    @Test
    @DisplayName("4. controlLevel = 100 termina la partida con ENDING_PAC_SYMBOL aunque lucidity también sea 0")
    void controlLevelCienTerminaConPacSymbol() {
        playthrough.setLucidity(40);
        playthrough.setControlLevel(55);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(playthrough));

        decisionService.createDecision(1L, "Stefan acepta la oferta de Mohan", ImpactLevel.CRITICO, OWNER_EMAIL, false);

        assertEquals(0, playthrough.getLucidity());
        assertEquals(100, playthrough.getControlLevel());
        assertEquals(PlaythroughStatus.FINALIZADA, playthrough.getStatus());
        assertEquals(EndingCode.ENDING_PAC_SYMBOL, playthrough.getEndingCode());
    }

    @Test
    @DisplayName("5. publishEvent() se llama una vez en una decisión normal y cero veces en una ENTRADA_CORRUPTA")
    void publishEventSoloEnDecisionesNoCorruptas() {
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(playthrough));
        when(nodeRepository.findByNodeCode("NODE-BUS")).thenReturn(Optional.of(currentNode));

        decisionService.createDecision(1L, "Stefan acepta la oferta de Mohan", ImpactLevel.LEVE, OWNER_EMAIL, false);
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        decisionService.createDecision(1L, "1984 !!! 2001", ImpactLevel.LEVE, OWNER_EMAIL, false);
        // La segunda decisión es ENTRADA_CORRUPTA: el conteo de eventos publicados no debe subir.
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));
    }
}
