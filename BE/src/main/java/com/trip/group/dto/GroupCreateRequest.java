package com.trip.group.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @Min(1) @Max(100) Integer maxMembers
) {}
