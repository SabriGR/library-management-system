package com.sabrigur.service;

import com.sabrigur.entity.Book;
import com.sabrigur.entity.Member;
import com.sabrigur.entity.Reservation;
import com.sabrigur.entity.ReservationStatus;
import com.sabrigur.exception.BusinessRuleException;
import com.sabrigur.exception.NotFoundException;
import com.sabrigur.repository.BookRepository;
import com.sabrigur.repository.MemberRepository;
import com.sabrigur.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final NotificationService notificationService;
    private final AuditService audit;
    private final Clock clock;

    /** A member can reserve a book only when no copy is available (FR-023). */
    @Transactional
    public Reservation reserve(Long memberId, Long bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("UserNotFound", "Member " + memberId + " not found"));
        if (!member.isActive()) {
            throw new BusinessRuleException("UserInactive", "The member account is inactive");
        }
        Book book = bookRepository.findByIdAndRemovedFalse(bookId)
                .orElseThrow(() -> new NotFoundException("BookNotFound", "Book " + bookId + " not found"));
        if (book.isAvailable()) {
            throw new BusinessRuleException("BookAvailable", "This book is available. Borrow it instead of reserving it");
        }
        if (reservationRepository.existsByMemberIdAndBookIdAndStatusIn(memberId, bookId,
                EnumSet.of(ReservationStatus.WAITING, ReservationStatus.READY))) {
            throw new BusinessRuleException("AlreadyReserved", "You already have an active reservation for this book");
        }
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .member(member).book(book).reservationDate(LocalDate.now(clock)).build());
        audit.log("RESERVE", "member=" + memberId + " book=" + bookId);
        return reservation;
    }

    /** A member can cancel only his or her own reservation (FR-024). */
    @Transactional
    public Reservation cancel(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .filter(r -> r.getMember().getId().equals(memberId))
                .orElseThrow(() -> new NotFoundException("ReservationNotFound", "Reservation " + reservationId + " not found"));
        reservation.cancel();
        audit.log("RESERVATION_CANCEL", "reservation=" + reservationId);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findByMember(Long memberId) {
        return reservationRepository.findByMemberIdOrderByReservationDateDescIdDesc(memberId);
    }

    @Transactional(readOnly = true)
    public boolean hasWaitingReservation(Long bookId) {
        return reservationRepository.existsByBookIdAndStatus(bookId, ReservationStatus.WAITING);
    }

    /** Called when a copy is returned. The first waiting member gets an e-mail (FR-025). */
    @Transactional
    public Optional<Reservation> notifyNextWaiting(Book book) {
        Optional<Reservation> next = reservationRepository
                .findFirstByBookIdAndStatusOrderByReservationDateAscIdAsc(book.getId(), ReservationStatus.WAITING);
        next.ifPresent(reservation -> {
            reservation.markReady();
            reservationRepository.save(reservation);
            Member member = reservation.getMember();
            notificationService.sendEmail(member.getEmail(), "Your reserved book is available",
                    "Dear " + member.getFullName() + ", the book '" + book.getTitle() + "' is now available.");
        });
        return next;
    }

    /** When a member borrows a book that was reserved, the reservation is completed. */
    @Transactional
    public void fulfillIfReady(Long memberId, Long bookId) {
        reservationRepository.findFirstByMemberIdAndBookIdAndStatus(memberId, bookId, ReservationStatus.READY)
                .ifPresent(r -> {
                    r.fulfill();
                    reservationRepository.save(r);
                });
    }
}
