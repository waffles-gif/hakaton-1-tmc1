package com.tuckersoft.branchengine.listener;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.DecisionStatus;
import com.tuckersoft.branchengine.model.LogStatus;
import com.tuckersoft.branchengine.model.RealityLog;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

/**
 * Corre en su propio hilo (branch-worker-N), después de que la transacción de la decisión
 * ya hizo commit. Por eso necesita su propia transacción (REQUIRES_NEW): la del hilo web
 * original ya no existe.
 */
@Component
public class BranchNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BranchNotificationListener.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_INSTANT;

    private final JavaMailSender mailSender;
    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;

    public BranchNotificationListener(JavaMailSender mailSender,
                                      DecisionRepository decisionRepository,
                                      RealityLogRepository realityLogRepository) {
        this.mailSender = mailSender;
        this.decisionRepository = decisionRepository;
        this.realityLogRepository = realityLogRepository;
    }

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent event) {
        Decision decision = decisionRepository.findById(event.decisionId()).orElse(null);
        if (decision == null) {
            return;
        }
        decision.setStatus(DecisionStatus.PROCESANDO);
        decisionRepository.save(decision);

        String subject = buildSubject(event);
        RealityLog realityLog = new RealityLog();
        realityLog.setDecision(decision);
        realityLog.setRecipientEmail(event.recipientEmail());
        realityLog.setSubject(subject);
        realityLog.setCreatedAt(Instant.now());

        try {
            if (event.simulateFailure()) {
                throw new org.springframework.mail.MailSendException(
                        "Simulación QA (X-Bandersnatch-Simulate: MAIL_FAILURE): envío forzado a fallar");
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.recipientEmail());
            message.setSubject(subject);
            message.setText(buildBody(event));
            mailSender.send(message);

            decision.setStatus(DecisionStatus.ESTABILIZADA);
            realityLog.setLogStatus(LogStatus.SENT);
            realityLog.setSentAt(Instant.now());
        } catch (MailException e) {
            decision.setStatus(DecisionStatus.ERROR);
            realityLog.setLogStatus(LogStatus.FAILED);
            realityLog.setErrorMessage(e.getMessage());
            log.error("Fallo al enviar el Informe de Realidad de la decisión {}", event.decisionId(), e);
        }

        decision.setUpdatedAt(Instant.now());
        decisionRepository.save(decision);
        realityLogRepository.save(realityLog);

        System.out.printf(
                "[BRANCH-LOG] Decision ID: %d | Player: %s | Branch: %s | Impact: %s | Unit: %s | Node: %s -> %s | Thread: %s | Status: %s%n",
                event.decisionId(), event.playerTag(), event.branchType(), event.impactLevel(), event.handlerUnit(),
                event.sourceNodeCode(), event.resolvedNodeCode(), Thread.currentThread().getName(), decision.getStatus());
    }

    private String buildSubject(DecisionCommittedEvent event) {
        return "[TUCKERSOFT] " + event.branchType() + " en " + event.playerTag() + " | Impacto " + event.impactLevel();
    }

    private String buildBody(DecisionCommittedEvent event) {
        String ending = event.endingCode() == null ? "-" : event.endingCode();
        return "Hola " + event.recipientDisplayName() + ",\n\n"
                + "Una partida de prueba acaba de ramificarse.\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                + "Decision ID      : #" + event.decisionId() + "\n"
                + "Jugador          : " + event.playerTag() + "\n"
                + "Rama             : " + event.branchType() + "\n"
                + "Impacto          : " + event.impactLevel() + "\n"
                + "Departamento     : " + event.handlerUnit() + "\n"
                + "Consecuencia     : " + event.outcomeCode() + "\n"
                + "Nodo origen      : " + event.sourceNodeCode() + "\n"
                + "Nodo destino     : " + event.resolvedNodeCode() + "\n"
                + "Estado partida   : " + event.playthroughStatus() + "\n"
                + "Lucidez          : " + event.lucidity() + "/100\n"
                + "Nivel de control : " + event.controlLevel() + "/100\n"
                + "Final            : " + ending + "\n"
                + "Registrada       : " + TIMESTAMP.format(event.decisionCreatedAt().atZone(ZoneOffset.UTC)) + "\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "Decisión original del jugador:\n"
                + "\"" + event.rawInput() + "\"\n\n"
                + "— Tuckersoft Branch Engine, 1984";
    }
}
