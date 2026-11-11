package com.sabrigur.service;

import com.sabrigur.dto.MemberActivityResponse;
import com.sabrigur.dto.MostBorrowedBookResponse;
import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.repository.BorrowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reports for librarians and administrators (FR-031). */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BorrowingRecordRepository borrowingRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<MostBorrowedBookResponse> mostBorrowedBooks(int limit) {
        return borrowingRepository.findMostBorrowed(PageRequest.of(0, Math.max(1, limit)));
    }

    @Transactional(readOnly = true)
    public List<MemberActivityResponse> memberActivity() {
        LocalDate today = LocalDate.now(clock);
        Map<Long, List<BorrowingRecord>> byMember = new LinkedHashMap<>();
        for (BorrowingRecord r : borrowingRepository.findAll()) {
            byMember.computeIfAbsent(r.getMember().getId(), k -> new java.util.ArrayList<>()).add(r);
        }
        return byMember.values().stream()
                .map(records -> new MemberActivityResponse(
                        records.get(0).getMember().getId(),
                        records.get(0).getMember().getFullName(),
                        records.size(),
                        records.stream().filter(r -> !r.isReturned()).count(),
                        records.stream().filter(r -> r.isOverdue(today)).count()))
                .sorted(Comparator.comparingLong(MemberActivityResponse::totalLoans).reversed())
                .toList();
    }
}
