package com.sabrigur.dto;

import com.sabrigur.entity.Fine;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FineResponse(Long id, Long recordId, Long memberId, String memberName, String bookTitle,
                           BigDecimal amount, boolean paid, LocalDate paymentDate) {

    public static FineResponse from(Fine f) {
        var record = f.getBorrowingRecord();
        return new FineResponse(f.getId(), record.getId(), record.getMember().getId(), record.getMember().getFullName(),
                record.getBook().getTitle(), f.getAmount(), f.isPaid(), f.getPaymentDate());
    }
}
