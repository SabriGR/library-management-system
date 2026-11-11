package com.sabrigur.controller;

import com.sabrigur.dto.BookRequest;
import com.sabrigur.dto.BookResponse;
import com.sabrigur.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /** UC-03 Search Books: by title, author, ISBN or category. Without a keyword all books are listed. */
    @GetMapping
    public List<BookResponse> search(@RequestParam(required = false) String keyword) {
        return bookService.search(keyword).stream().map(BookResponse::from).toList();
    }

    /** UC-04 View Book Details. */
    @GetMapping("/{id}")
    public BookResponse get(@PathVariable Long id) {
        return BookResponse.from(bookService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<BookResponse> add(@Valid @RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.from(bookService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return BookResponse.from(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        bookService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
