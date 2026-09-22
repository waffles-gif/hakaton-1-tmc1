package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DecisionCreateRequest(
        @NotNull Long playthroughId,
        @NotBlank @Size(min = 10) String rawInput,
        @NotBlank @Pattern(regexp = "LEVE|MODERADO|GRAVE|CRITICO") String impactLevel
) {
}
