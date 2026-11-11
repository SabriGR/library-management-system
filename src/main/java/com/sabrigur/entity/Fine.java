package com.sabrigur.entity;

import com.sabrigur.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "fines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_record_id", nullable = false, unique = true)
    private BorrowingRecord borrowingRecord;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private boolean paid;

    private LocalDate paymentDate;

    /** Fine = late days x daily rate (FR-027). */
    public static BigDecimal calculateAmount(long daysLate, BigDecimal ratePerDay) {
        return ratePerDay.multiply(BigDecimal.valueOf(daysLate)).setScale(2, RoundingMode.HALF_UP);
    }

    public void pay(LocalDate date) {
        if (paid) {
            throw new BusinessRuleException("FineAlreadyPaid", "This fine is already paid");
        }
        this.paid = true;
        this.paymentDate = date;
    }
}
