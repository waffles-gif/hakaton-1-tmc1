package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.NodeCreateRequest;
import com.tuckersoft.branchengine.dto.NodeResponse;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.NotFoundException;
import com.tuckersoft.branchengine.model.Node;
import com.tuckersoft.branchengine.repository.NodeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NodeService {

    private final NodeRepository nodeRepository;

    public NodeService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Transactional
    public NodeResponse create(NodeCreateRequest request) {
        if (nodeRepository.existsByNodeCode(request.nodeCode())) {
            throw new ConflictException("Ya existe un nodo con ese código: " + request.nodeCode());
        }
        Node node = new Node();
        node.setNodeCode(request.nodeCode());
        node.setTitle(request.title());
        node.setSceneText(request.sceneText());
        node.setBranchCapacity(request.branchCapacity());
        node.setCurrentBranches(0);
        node.setPrimaryBranchCode(request.primaryBranchCode());
        node.setGlitchBranchCode(request.glitchBranchCode());
        node.setCreatedAt(Instant.now());
        return NodeResponse.from(nodeRepository.save(node));
    }

    @Transactional(readOnly = true)
    public List<NodeResponse> listAll() {
        return nodeRepository.findAll().stream().map(NodeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NodeResponse getById(Long id) {
        return NodeResponse.from(findByIdOrThrow(id));
    }

    Node findByIdOrThrow(Long id) {
        return nodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nodo no encontrado: " + id));
    }

    Node findByNodeCodeOrThrow(String nodeCode) {
        return nodeRepository.findByNodeCode(nodeCode)
                .orElseThrow(() -> new NotFoundException("Nodo no encontrado: " + nodeCode));
    }

    void save(Node node) {
        nodeRepository.save(node);
    }
}
