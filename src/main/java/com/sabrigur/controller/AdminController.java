package com.sabrigur.controller;

import com.sabrigur.dto.RulesRequest;
import com.sabrigur.dto.RulesResponse;
import com.sabrigur.dto.StaffRequest;
import com.sabrigur.dto.UserResponse;
import com.sabrigur.service.RulesService;
import com.sabrigur.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administrator functions: UC-25 Manage Staff Accounts and UC-26 Configure Library Rules. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StaffService staffService;
    private final RulesService rulesService;

    @GetMapping("/staff")
    public List<UserResponse> listStaff() {
        return staffService.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping("/staff")
    public ResponseEntity<UserResponse> createStaff(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(staffService.create(request)));
    }

    @PatchMapping("/staff/{id}/deactivate")
    public UserResponse deactivateStaff(@PathVariable Long id, Authentication auth) {
        return UserResponse.from(staffService.setActive(id, false, auth.getName()));
    }

    @PatchMapping("/staff/{id}/activate")
    public UserResponse activateStaff(@PathVariable Long id, Authentication auth) {
        return UserResponse.from(staffService.setActive(id, true, auth.getName()));
    }

    @GetMapping("/rules")
    public RulesResponse getRules() {
        return RulesResponse.from(rulesService.getRules());
    }

    @PutMapping("/rules")
    public RulesResponse updateRules(@Valid @RequestBody RulesRequest request) {
        return RulesResponse.from(rulesService.update(request));
    }
}
