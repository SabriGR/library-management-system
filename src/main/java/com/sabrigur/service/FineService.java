package com.sabrigur.service;

import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.entity.Fine;
import com.sabrigur.exception.NotFoundException;
import com.sabrigur.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRepository fineRepository;
    private final Clock clock;
    private final AuditService audit;

    /** Creates the fine of a record that was returned late (FR-027). */
    @Transactional
    public Fine createFine(BorrowingRecord record, long daysLate, BigDecimal ratePerDay) {
        Fine fine = Fine.builder()
                .borrowingRecord(record)
                .amount(Fine.calculateAmount(daysLate, ratePerDay))
                .build();
        record.setFine(fine);
        audit.log("FINE_CREATED", "record=" + record.getId() + " daysLate=" + daysLate + " amount=" + fine.getAmount());
        return fineRepository.save(fine);
    }

    /** Marks a fine as paid and stores the payment date (FR-028). */
    @Transactional
    public Fine pay(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new NotFoundException("FineNotFound", "Fine " + fineId + " not found"));
        fine.pay(LocalDate.now(clock));
        audit.log("FINE_PAID", "fine=" + fineId + " amount=" + fine.getAmount());
        return fineRepository.save(fine);
    }

    @Transactional(readOnly = true)
    public List<Fine> findUnpaid(Long memberId) {
        return memberId == null ? fineRepository.findByPaidFalse()
                : fineRepository.findByBorrowingRecordMemberIdAndPaidFalse(memberId);
    }

    @Transactional(readOnly = true)
    public List<Fine> findByMember(Long memberId) {
        return fineRepository.findByBorrowingRecordMemberIdOrderByIdDesc(memberId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalUnpaid(Long memberId) {
        return fineRepository.findByBorrowingRecordMemberIdAndPaidFalse(memberId).stream()
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
