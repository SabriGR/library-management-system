package com.sabrigur.controller;

import com.sabrigur.dto.FineResponse;
import com.sabrigur.security.LibraryUserDetails;
import com.sabrigur.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    /** All unpaid fines, or the unpaid fines of one member. */
    @GetMapping("/unpaid")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public List<FineResponse> unpaid(@RequestParam(required = false) Long memberId) {
        return fineService.findUnpaid(memberId).stream().map(FineResponse::from).toList();
    }

    /** UC-21 Record Fine Payment. */
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public FineResponse pay(@PathVariable Long id) {
        return FineResponse.from(fineService.pay(id));
    }

    /** The fines of the logged-in member. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('MEMBER')")
    public List<FineResponse> myFines(@AuthenticationPrincipal LibraryUserDetails me) {
        return fineService.findByMember(me.getId()).stream().map(FineResponse::from).toList();
    }
}
