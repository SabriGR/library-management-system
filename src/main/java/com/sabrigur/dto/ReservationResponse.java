package com.sabrigur.dto;

import com.sabrigur.entity.Reservation;

import java.time.LocalDate;

public record ReservationResponse(Long id, Long memberId, Long bookId, String bookTitle,
                                  LocalDate reservationDate, String status) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getMember().getId(), r.getBook().getId(), r.getBook().getTitle(),
                r.getReservationDate(), r.getStatus().name());
    }
}
