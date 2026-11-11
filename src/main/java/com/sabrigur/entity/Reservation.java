package com.sabrigur.entity;

import com.sabrigur.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private LocalDate reservationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.WAITING;

    public boolean isActive() {
        return status == ReservationStatus.WAITING || status == ReservationStatus.READY;
    }

    public void cancel() {
        if (!isActive()) {
            throw new BusinessRuleException("ReservationNotActive", "Only an active reservation can be cancelled");
        }
        status = ReservationStatus.CANCELLED;
    }

    public void markReady() {
        status = ReservationStatus.READY;
    }

    public void fulfill() {
        status = ReservationStatus.FULFILLED;
    }
}
