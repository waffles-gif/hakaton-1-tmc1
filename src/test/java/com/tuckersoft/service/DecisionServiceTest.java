package com.tuckersoft.service;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.model.*;
import com.tuckersoft.branchengine.repository.*;
import com.tuckersoft.branchengine.service.DecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock private DecisionRepository decisionRepository;
    @Mock private RealityLogRepository realityLogRepository;
    @Mock private GameSessionRepository gameSessionRepository;
    @Mock private NodeRepository nodeRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DecisionService decisionService;

    private GameSession activeSession;
    private Node currentNode;

    @BeforeEach
    void setUp() {
        currentNode = new Node();
        currentNode.setId(10L);
        currentNode.setTerminal(false);

        User user = new User();
        user.setEmail("player@tuckersoft.com");

        activeSession = new GameSession();
        activeSession.setId(1L);
        activeSession.setStatus("ACTIVE");
        activeSession.setLucidity(50);
        activeSession.setControlLevel(50);
        activeSession.setCurrentNode(currentNode);
        activeSession.setUser(user);
    }

    @Test
    @DisplayName("Test 1: Normalización e inferencia de ACCEPTANCE cuando la entrada contiene 'Sí'")
    void testProcessDecision_Acceptance() {
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(nodeRepository.findBySourceNodeAndOutcomeCode(eq(currentNode), eq("ACCEPTANCE")))
                .thenReturn(Optional.of(currentNode));
        when(decisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Decision decision = decisionService.processDecision(1L, "¡Sí, acepto!", false);

        assertEquals("ACCEPTANCE", decision.getOutcomeCode());
        assertEquals("UNIT_ALPHA", decision.getHandlerUnit());
        assertEquals("si, acepto!", decision.getNormalizedText());
    }

    @Test
    @DisplayName("Test 2: Límites de stats clamped a [0, 100]")
    void testProcessDecision_StatClamping() {
        activeSession.setLucidity(5);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(decisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        decisionService.processDecision(1L, "no quiero", false);

        assertEquals(0, activeSession.getLucidity());
    }

    @Test
    @DisplayName("Test 3: Transición de estado a INSANITY_ENDING si Lucidity alcanza 0")
    void testProcessDecision_InsanityEnding() {
        activeSession.setLucidity(10);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(decisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        decisionService.processDecision(1L, "luchar contra el sistema", false);

        assertEquals("INSANITY_ENDING", activeSession.getStatus());
    }

    @Test
    @DisplayName("Test 4: Excepción al intentar procesar una sesión inactiva")
    void testProcessDecision_InactiveSession_ThrowsException() {
        activeSession.setStatus("COMPLETED");
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));

        assertThrows(IllegalStateException.class, () ->
                decisionService.processDecision(1L, "aceptar", false)
        );
    }

    @Test
    @DisplayName("Test 5: Publicación de DecisionCommittedEvent tras persistir exitosamente")
    void testProcessDecision_PublishesEvent() {
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(decisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        decisionService.processDecision(1L, "aceptar", true);

        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));
    }
}