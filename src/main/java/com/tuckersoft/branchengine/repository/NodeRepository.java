package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {

    Optional<Node> findByNodeCode(String nodeCode);

    boolean existsByNodeCode(String nodeCode);

    @Query("SELECT n FROM Node n WHERE n.id = :sourceNode AND n.nodeCode = :outcomeCode")
    Optional<Node> findBySourceNodeAndOutcomeCode(@Param("sourceNode") Node sourceNode,
                                                  @Param("outcomeCode") String outcomeCode);
}
