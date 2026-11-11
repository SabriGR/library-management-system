package com.sabrigur.controller;

import com.sabrigur.dto.BorrowingRecordResponse;
import com.sabrigur.security.LibraryUserDetails;
import com.sabrigur.service.LibraryService;
import com.sabrigur.service.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;
    private final NotificationService notificationService;

    /** UC-15 Borrow Book. */
    @PostMapping("/borrow")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<BorrowingRecordResponse> borrowBook(@RequestParam Long userId, @RequestParam Long bookId) {
        return ResponseEntity.ok(BorrowingRecordResponse.from(libraryService.borrowBook(userId, bookId)));
    }

    /** UC-18 Return Book. */
    @PostMapping("/return/{recordId}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<BorrowingRecordResponse> returnBook(@PathVariable Long recordId) {
        return ResponseEntity.ok(BorrowingRecordResponse.from(libraryService.returnBook(recordId)));
    }

    /** UC-09 Renew Loan: a member renews his or her own loan. */
    @PostMapping("/renew/{recordId}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<BorrowingRecordResponse> renewLoan(@PathVariable Long recordId,
                                                             @AuthenticationPrincipal LibraryUserDetails me) {
        return ResponseEntity.ok(BorrowingRecordResponse.from(libraryService.renewLoan(recordId, me.getId())));
    }

    /** UC-07 View Active Loans. A member can see only his or her own loans. */
    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasRole('LIBRARIAN') or #userId == principal.id")
    public ResponseEntity<List<BorrowingRecordResponse>> getActiveBorrowings(@PathVariable Long userId) {
        return ResponseEntity.ok(libraryService.getActiveBorrowingsByUser(userId).stream()
                .map(BorrowingRecordResponse::from).toList());
    }

    /** UC-08 View Borrowing History. */
    @GetMapping("/user/{userId}/history")
    @PreAuthorize("hasRole('LIBRARIAN') or #userId == principal.id")
    public ResponseEntity<List<BorrowingRecordResponse>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(libraryService.getBorrowingHistory(userId).stream()
                .map(BorrowingRecordResponse::from).toList());
    }

    /** UC-22 View Overdue Loans. */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<List<BorrowingRecordResponse>> getOverdueBooks() {
        return ResponseEntity.ok(libraryService.getOverdueBooks().stream()
                .map(BorrowingRecordResponse::from).toList());
    }

    /** UC-23 Send Overdue Reminder. */
    @PostMapping("/overdue/remind")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Map<String, Integer>> sendOverdueReminders() {
        return ResponseEntity.ok(Map.of("emailsSent", notificationService.sendOverdueReminders()));
    }
}
