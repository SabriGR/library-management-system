package com.sabrigur.repository;

import com.sabrigur.entity.Reservation;
import com.sabrigur.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberIdOrderByReservationDateDescIdDesc(Long memberId);

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, Collection<ReservationStatus> statuses);

    boolean existsByBookIdAndStatus(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservationDateAscIdAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, ReservationStatus status);
}
