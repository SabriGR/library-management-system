package com.sabrigur.service;

import com.sabrigur.entity.Book;
import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.entity.User;
import com.sabrigur.repository.BookRepository;
import com.sabrigur.repository.BorrowingRecordRepository;
import com.sabrigur.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowingRecordRepository borrowingRepository;

    @Transactional
    public BorrowingRecord borrowBook(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("UserNotFound"));
                
        if (!user.isActive()) {
            throw new RuntimeException("UserInactive");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("BookNotFound"));

        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("BookNotAvailable");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        BorrowingRecord record = BorrowingRecord.builder()
                .user(user)
                .book(book)
                .borrowDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(14))
                .build();

        return borrowingRepository.save(record);
    }

    @Transactional
    public BorrowingRecord returnBook(Long recordId) {
        BorrowingRecord record = borrowingRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("RecordNotFound"));

        if (record.getReturnDate() != null) {
            throw new RuntimeException("BookAlreadyReturned");
        }

        record.setReturnDate(LocalDate.now());
        
        Book book = record.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        
        bookRepository.save(book);
        return borrowingRepository.save(record);
    }

    public List<BorrowingRecord> getActiveBorrowingsByUser(Long userId) {
        return borrowingRepository.findByUserIdAndReturnDateIsNull(userId);
    }

    public List<BorrowingRecord> getOverdueBooks() {
        return borrowingRepository.findOverdueBorrowings(LocalDate.now());
    }
}