package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.Node;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NodeRepository extends JpaRepository<Node, Long> {

    Optional<Node> findByNodeCode(String nodeCode);

    boolean existsByNodeCode(String nodeCode);
}
