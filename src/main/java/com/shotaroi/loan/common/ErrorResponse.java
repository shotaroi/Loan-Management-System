package com.shotaroi.loan.common;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}
