package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.RealityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RealityLogRepository extends JpaRepository<RealityLog, Long> {

    List<RealityLog> findByDecisionIdOrderByIdAsc(Long decisionId);
}
