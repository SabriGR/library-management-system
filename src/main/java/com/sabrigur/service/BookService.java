package com.sabrigur.service;

import com.sabrigur.dto.BookRequest;
import com.sabrigur.entity.Book;
import com.sabrigur.exception.BusinessRuleException;
import com.sabrigur.exception.NotFoundException;
import com.sabrigur.repository.BookRepository;
import com.sabrigur.repository.BorrowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Catalog management (FR-004 to FR-008). */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BorrowingRecordRepository borrowingRepository;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<Book> search(String keyword) {
        return bookRepository.search(keyword == null ? "" : keyword.trim());
    }

    @Transactional(readOnly = true)
    public Book get(Long id) {
        return bookRepository.findByIdAndRemovedFalse(id)
                .orElseThrow(() -> new NotFoundException("BookNotFound", "Book " + id + " not found"));
    }

    @Transactional
    public Book create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new BusinessRuleException("DuplicateIsbn", "A book with ISBN " + request.isbn() + " already exists");
        }
        Book book = bookRepository.save(Book.builder()
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .category(request.category())
                .totalCopies(request.totalCopies())
                .availableCopies(request.totalCopies())
                .build());
        audit.log("BOOK_ADD", "book=" + book.getId() + " isbn=" + book.getIsbn());
        return book;
    }

    @Transactional
    public Book update(Long id, BookRequest request) {
        Book book = get(id);
        if (!book.getIsbn().equals(request.isbn()) && bookRepository.existsByIsbn(request.isbn())) {
            throw new BusinessRuleException("DuplicateIsbn", "A book with ISBN " + request.isbn() + " already exists");
        }
        int onLoan = book.getTotalCopies() - book.getAvailableCopies();
        if (request.totalCopies() < onLoan) {
            throw new BusinessRuleException("InvalidCopies",
                    "Total copies cannot be less than the " + onLoan + " copies that are on loan");
        }
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setCategory(request.category());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(request.totalCopies() - onLoan);
        audit.log("BOOK_UPDATE", "book=" + id);
        return bookRepository.save(book);
    }

    /** A book can be removed only if no copy is on loan (FR-006). The borrowing history is kept. */
    @Transactional
    public void remove(Long id) {
        Book book = get(id);
        if (borrowingRepository.existsByBookIdAndReturnDateIsNull(id)) {
            throw new BusinessRuleException("BookOnLoan", "The book cannot be removed while a copy is on loan");
        }
        book.setRemoved(true);
        bookRepository.save(book);
        audit.log("BOOK_REMOVE", "book=" + id);
    }
}
