package com.sabrigur.controller;

import com.sabrigur.dto.MemberDetailsResponse;
import com.sabrigur.dto.MemberUpdateRequest;
import com.sabrigur.dto.RegisterRequest;
import com.sabrigur.dto.UserResponse;
import com.sabrigur.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** UC-14 Manage Members (librarians only). */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LIBRARIAN')")
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public List<UserResponse> search(@RequestParam(required = false) String keyword) {
        return memberService.search(keyword).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MemberDetailsResponse details(@PathVariable Long id) {
        return memberService.details(id);
    }

    @PostMapping
    public ResponseEntity<UserResponse> add(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(memberService.register(request)));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) {
        return UserResponse.from(memberService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id) {
        return UserResponse.from(memberService.setActive(id, false));
    }

    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id) {
        return UserResponse.from(memberService.setActive(id, true));
    }
}
