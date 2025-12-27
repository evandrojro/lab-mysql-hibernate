package com.txlab.api.dto;

public record DualOperationResponse(
        Long userId,
        Long auditId,
        String message
) {
}
