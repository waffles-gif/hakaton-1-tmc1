package com.tuckersoft.branchengine.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reality_logs")
public class RealityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameSessionId;

    private Integer lucidityDelta;
    private Integer controlLevelDelta;
    private String outcomeCode;

    private LocalDateTime loggedAt;

    @PrePersist
    public void prePersist() {
        this.loggedAt = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGameSessionId() { return gameSessionId; }
    public void setGameSessionId(Long gameSessionId) { this.gameSessionId = gameSessionId; }
    public Integer getLucidityDelta() { return lucidityDelta; }
    public void setLucidityDelta(Integer lucidityDelta) { this.lucidityDelta = lucidityDelta; }
    public Integer getControlLevelDelta() { return controlLevelDelta; }
    public void setControlLevelDelta(Integer controlLevelDelta) { this.controlLevelDelta = controlLevelDelta; }
    public String getOutcomeCode() { return outcomeCode; }
    public void setOutcomeCode(String outcomeCode) { this.outcomeCode = outcomeCode; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}