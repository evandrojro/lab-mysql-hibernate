package com.txlab.api.dto;

public record OutboxEnqueueResponse(
        Long userId,
        Long outboxEventId,
        String message
) {
}
