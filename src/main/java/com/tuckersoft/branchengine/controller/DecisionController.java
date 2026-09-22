package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.DecisionCreateRequest;
import com.tuckersoft.branchengine.dto.DecisionPageResponse;
import com.tuckersoft.branchengine.dto.DecisionResponse;
import com.tuckersoft.branchengine.dto.RealityLogResponse;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.Roles;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.service.DecisionService;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
public class DecisionController {

    private static final String SIMULATE_HEADER = "X-Bandersnatch-Simulate";
    private static final String MAIL_FAILURE = "MAIL_FAILURE";

    private final DecisionService decisionService;
    private final RealityLogRepository realityLogRepository;

    public DecisionController(DecisionService decisionService, RealityLogRepository realityLogRepository) {
        this.decisionService = decisionService;
        this.realityLogRepository = realityLogRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionResponse create(@Valid @RequestBody DecisionCreateRequest request,
                                   @RequestHeader(value = SIMULATE_HEADER, required = false) String simulateHeader,
                                   Authentication authentication) {
        boolean simulateFailure = MAIL_FAILURE.equals(simulateHeader);
        Decision decision = decisionService.createDecision(request.playthroughId(), request.rawInput(),
                request.impactLevel(), authentication.getName(), simulateFailure);
        return DecisionResponse.from(decision);
    }

    @GetMapping
    public DecisionPageResponse list(@RequestParam(required = false) String branchType,
                                     @RequestParam(required = false) String impactLevel,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) Long playthroughId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     Authentication authentication) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = decisionService.search(authentication.getName(), isAdmin(authentication),
                branchType, impactLevel, status, playthroughId, pageable);
        return DecisionPageResponse.from(result);
    }

    @GetMapping("/{id}")
    public DecisionResponse getById(@PathVariable Long id, Authentication authentication) {
        return DecisionResponse.from(decisionService.getOwnedOrThrow(id, authentication.getName(), isAdmin(authentication)));
    }

    @GetMapping("/{id}/reality-logs")
    public List<RealityLogResponse> realityLogs(@PathVariable Long id, Authentication authentication) {
        decisionService.getOwnedOrThrow(id, authentication.getName(), isAdmin(authentication));
        return realityLogRepository.findByDecisionIdOrderByIdAsc(id).stream().map(RealityLogResponse::from).toList();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority(Roles.ADMIN));
    }
}
