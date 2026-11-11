package com.sabrigur.exception;

import java.time.Instant;

/** JSON body returned for every error (NFR-010: clear error messages). */
public record ErrorResponse(Instant timestamp, int status, String code, String message) {

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(Instant.now(), status, code, message);
    }
}
