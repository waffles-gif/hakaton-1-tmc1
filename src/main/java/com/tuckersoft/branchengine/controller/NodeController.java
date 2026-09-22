package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.NodeCreateRequest;
import com.tuckersoft.branchengine.dto.NodeResponse;
import com.tuckersoft.branchengine.service.NodeService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NodeResponse create(@Valid @RequestBody NodeCreateRequest request) {
        return nodeService.create(request);
    }

    @GetMapping
    public List<NodeResponse> listAll() {
        return nodeService.listAll();
    }

    @GetMapping("/{id}")
    public NodeResponse getById(@PathVariable Long id) {
        return nodeService.getById(id);
    }
}
