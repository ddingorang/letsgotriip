package com.trip.global.error;

public record ErrorResponse(
        String code,
        String message
) {
}
