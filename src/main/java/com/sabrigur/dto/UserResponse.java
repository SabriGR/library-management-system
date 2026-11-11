package com.sabrigur.dto;

import com.sabrigur.entity.User;

public record UserResponse(Long id, String firstName, String lastName, String email, String phoneNumber,
                           boolean active, String role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhoneNumber(),
                u.isActive(), u.getRole());
    }
}
