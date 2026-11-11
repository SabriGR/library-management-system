package com.sabrigur.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phoneNumber) {
}
