package com.sabrigur.controller;

import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.service.LibraryService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @PostMapping("/borrow")
    public ResponseEntity<BorrowingRecord> borrowBook(@RequestParam Long userId, @RequestParam Long bookId) {
        return ResponseEntity.ok(libraryService.borrowBook(userId, bookId));
    }

    @PostMapping("/return/{recordId}")
    public ResponseEntity<BorrowingRecord> returnBook(@PathVariable Long recordId) {
        return ResponseEntity.ok(libraryService.returnBook(recordId));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<BorrowingRecord>> getActiveBorrowings(@PathVariable Long userId) {
        return ResponseEntity.ok(libraryService.getActiveBorrowingsByUser(userId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<BorrowingRecord>> getOverdueBooks() {
        return ResponseEntity.ok(libraryService.getOverdueBooks());
    }
}