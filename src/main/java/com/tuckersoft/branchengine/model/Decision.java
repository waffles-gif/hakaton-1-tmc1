package com.tuckersoft.branchengine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playthrough_id", nullable = false)
    private GameSession playthrough;

    /** Nodo de origen: el currentNode de la partida al momento de decidir. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawInput;

    @Column(nullable = false, length = 40)
    private String branchType;

    @Column(nullable = false, length = 20)
    private String impactLevel;

    @Column(nullable = false, length = 60)
    private String handlerUnit;

    @Column(nullable = false, length = 60)
    private String outcomeCode;

    private String resolvedNodeCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
