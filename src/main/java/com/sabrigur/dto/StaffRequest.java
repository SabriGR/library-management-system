package com.sabrigur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String phoneNumber,
        @NotBlank @Size(min = 8, message = "must have at least 8 characters") String password,
        String employeeNo) {
}
