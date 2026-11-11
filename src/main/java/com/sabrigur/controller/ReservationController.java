package com.sabrigur.controller;

import com.sabrigur.dto.ReservationResponse;
import com.sabrigur.security.LibraryUserDetails;
import com.sabrigur.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Reservations of the logged-in member (UC-05, UC-06). */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@RequestParam Long bookId,
                                                       @AuthenticationPrincipal LibraryUserDetails me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservationResponse.from(reservationService.reserve(me.getId(), bookId)));
    }

    @DeleteMapping("/{id}")
    public ReservationResponse cancel(@PathVariable Long id, @AuthenticationPrincipal LibraryUserDetails me) {
        return ReservationResponse.from(reservationService.cancel(id, me.getId()));
    }

    @GetMapping("/my")
    public List<ReservationResponse> myReservations(@AuthenticationPrincipal LibraryUserDetails me) {
        return reservationService.findByMember(me.getId()).stream().map(ReservationResponse::from).toList();
    }
}
