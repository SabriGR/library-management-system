package com.sabrigur.controller;

import com.sabrigur.dto.BorrowingRecordResponse;
import com.sabrigur.dto.MemberActivityResponse;
import com.sabrigur.dto.MostBorrowedBookResponse;
import com.sabrigur.service.LibraryService;
import com.sabrigur.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** UC-24 Generate Reports. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LIBRARIAN')")
public class ReportController {

    private final ReportService reportService;
    private final LibraryService libraryService;

    @GetMapping("/most-borrowed")
    public List<MostBorrowedBookResponse> mostBorrowed(@RequestParam(defaultValue = "5") int limit) {
        return reportService.mostBorrowedBooks(limit);
    }

    @GetMapping("/overdue")
    public List<BorrowingRecordResponse> overdue() {
        return libraryService.getOverdueBooks().stream().map(BorrowingRecordResponse::from).toList();
    }

    @GetMapping("/member-activity")
    public List<MemberActivityResponse> memberActivity() {
        return reportService.memberActivity();
    }
}
