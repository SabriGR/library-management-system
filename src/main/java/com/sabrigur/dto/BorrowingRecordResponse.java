package com.sabrigur.dto;

import com.sabrigur.entity.BorrowingRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BorrowingRecordResponse(Long id, Long memberId, String memberName, Long bookId, String bookTitle,
                                      LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
                                      int renewalCount, boolean overdue, BigDecimal fineAmount) {

    public static BorrowingRecordResponse from(BorrowingRecord r) {
        return new BorrowingRecordResponse(r.getId(), r.getMember().getId(), r.getMember().getFullName(),
                r.getBook().getId(), r.getBook().getTitle(), r.getBorrowDate(), r.getDueDate(), r.getReturnDate(),
                r.getRenewalCount(), r.isOverdue(LocalDate.now()),
                r.getFine() == null ? null : r.getFine().getAmount());
    }
}
