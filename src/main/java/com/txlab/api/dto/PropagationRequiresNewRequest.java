package com.txlab.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PropagationRequiresNewRequest(
        @NotBlank String name,
        @NotEmpty List<String> items,
        @NotNull Boolean failOuter,
        String failOnItem
) {
}
