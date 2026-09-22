package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long>, JpaSpecificationExecutor<Decision> {

    List<Decision> findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(Long playthroughId);
}
