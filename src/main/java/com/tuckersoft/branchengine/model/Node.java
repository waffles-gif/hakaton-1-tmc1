package com.tuckersoft.branchengine.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "story_nodes")
@Getter
@Setter
@NoArgsConstructor
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nodeCode;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sceneText;

    @Column(nullable = false)
    private Integer branchCapacity;

    @Column(nullable = false)
    private Integer currentBranches = 0;

    private String primaryBranchCode;

    private String glitchBranchCode;

    @Column(nullable = false)
    private Instant createdAt;

    // --- AGREGADO PARA EL CHECKPOINT ★4 / DECISION SERVICE ---

    // Indica si el nodo es final/terminal
    @Column(nullable = false)
    private boolean terminal = false;

    // Helper getter para responder a isTerminal() si Lombok genera getTerminal()
    public boolean isTerminal() {
        return terminal;
    }

    // Helper getter si algún servicio llama a getCode()
    public String getCode() {
        return nodeCode;
    }
}