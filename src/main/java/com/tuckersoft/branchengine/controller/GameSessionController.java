package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.PlaythroughCreateRequest;
import com.tuckersoft.branchengine.dto.PlaythroughPathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.model.Roles;
import com.tuckersoft.branchengine.service.GameSessionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
public class GameSessionController {

    private final GameSessionService gameSessionService;

    public GameSessionController(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaythroughResponse create(@Valid @RequestBody PlaythroughCreateRequest request,
                                       Authentication authentication) {
        return gameSessionService.create(request, authentication.getName());
    }

    @GetMapping
    public List<PlaythroughResponse> listAll(Authentication authentication) {
        return gameSessionService.listForUser(authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{id}")
    public PlaythroughResponse getById(@PathVariable Long id, Authentication authentication) {
        return gameSessionService.getById(id, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{id}/path")
    public PlaythroughPathResponse getPath(@PathVariable Long id, Authentication authentication) {
        return gameSessionService.getPath(id, authentication.getName(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority(Roles.ADMIN));
    }
}
