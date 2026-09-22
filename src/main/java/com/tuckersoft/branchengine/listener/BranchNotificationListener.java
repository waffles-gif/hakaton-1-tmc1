package com.tuckersoft.branchengine.listener;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BranchNotificationListener {

    private final JavaMailSender mailSender;

    public BranchNotificationListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDecisionCommitted(DecisionCommittedEvent event) {
        // Manejo del header X-Bandersnatch-Simulate
        if (event.simulateHeaderPresent()) {
            System.out.println("[SIMULATION] Modo simulación activo. Correo omitido para: " + event.playerEmail());
            return;
        }

        if (event.playerEmail() == null || event.playerEmail().isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.playerEmail());
        message.setSubject("Bandersnatch - Actualización de Rama");
        message.setText(String.format("Tu decisión ha sido procesada.\nUnidad: %s\nResultado: %s",
                event.handlerUnit(), event.outcomeCode()));

        mailSender.send(message);
    }
}
