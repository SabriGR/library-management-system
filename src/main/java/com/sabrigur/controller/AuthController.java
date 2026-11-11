package com.sabrigur.controller;

import com.sabrigur.dto.RegisterRequest;
import com.sabrigur.dto.UserResponse;
import com.sabrigur.exception.NotFoundException;
import com.sabrigur.repository.UserRepository;
import com.sabrigur.security.LibraryUserDetails;
import com.sabrigur.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Registration (UC-01) and login check (UC-02). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final UserRepository userRepository;

    /** Public: a new user creates a member account. */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(memberService.register(request)));
    }

    /** Log in: the credentials are checked by HTTP Basic authentication. Returns the profile and the role. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal LibraryUserDetails principal) {
        return userRepository.findById(principal.getId())
                .map(u -> ResponseEntity.ok(UserResponse.from(u)))
                .orElseThrow(() -> new NotFoundException("UserNotFound", "User not found"));
    }
}
