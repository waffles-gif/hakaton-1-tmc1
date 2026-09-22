package com.tuckersoft.branchengine.repository;
import com.tuckersoft.branchengine.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long> {}