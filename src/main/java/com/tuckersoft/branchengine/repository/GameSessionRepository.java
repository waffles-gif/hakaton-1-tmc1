package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.GameSession;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    boolean existsByPlayerTag(String playerTag);

    List<GameSession> findAllByUserEmailOrderByCreatedAtDesc(String email);

    List<GameSession> findAllByOrderByCreatedAtDesc();
}
