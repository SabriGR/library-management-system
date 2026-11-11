package com.sabrigur.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "borrowing_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowingRecord {

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
    private LocalDate borrowDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Builder.Default
    private int renewalCount = 0;

    /** A fine exists only because of this record (composition in the class diagram). */
    @OneToOne(mappedBy = "borrowingRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private Fine fine;

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean isOverdue(LocalDate today) {
        return returnDate == null && today.isAfter(dueDate);
    }

    public void markReturned(LocalDate date) {
        this.returnDate = date;
    }

    public void renew(int days) {
        this.dueDate = dueDate.plusDays(days);
        this.renewalCount++;
    }

    /** Number of days between the due date and the given date, never negative. */
    public long daysLate(LocalDate date) {
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, date));
    }
}
