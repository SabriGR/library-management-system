package com.sabrigur.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotBlank String isbn,
        String category,
        @Min(value = 1, message = "must be at least 1") int totalCopies) {
}
