package com.sabrigur.service;

import com.sabrigur.entity.Book;
import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.entity.LibraryRules;
import com.sabrigur.entity.Member;
import com.sabrigur.exception.BusinessRuleException;
import com.sabrigur.exception.NotFoundException;
import com.sabrigur.repository.BookRepository;
import com.sabrigur.repository.BorrowingRecordRepository;
import com.sabrigur.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Circulation: borrow, return and renew books (FR-013 to FR-022). */
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BorrowingRecordRepository borrowingRepository;
    private final RulesService rulesService;
    private final FineService fineService;
    private final ReservationService reservationService;
    private final AuditService audit;
    private final Clock clock;

    /**
     * Use case "Borrow Book" (UC-15). The order of the checks is the same as in the sequence and activity diagrams.
     */
    @Transactional
    public BorrowingRecord borrowBook(Long userId, Long bookId) {
        Member member = memberRepository.findById(userId).orElse(null);
        if (member == null) {                                                    // D1: member exists?
            throw new NotFoundException("UserNotFound", "Member " + userId + " not found");
        }
        if (!member.isActive()) {                                                // D2: member active?
            throw new BusinessRuleException("UserInactive", "The member account is inactive");
        }

        LibraryRules rules = rulesService.getRules();
        long activeLoans = borrowingRepository.countByMemberIdAndReturnDateIsNull(userId);
        if (activeLoans >= rules.getMaxLoansPerMember()) {                       // D3: loan limit reached?
            throw new BusinessRuleException("LoanLimitReached",
                    "The member already has " + activeLoans + " active loans (limit " + rules.getMaxLoansPerMember() + ")");
        }

        Book book = bookRepository.findByIdAndRemovedFalse(bookId).orElse(null);
        if (book == null) {                                                      // D4: book exists?
            throw new NotFoundException("BookNotFound", "Book " + bookId + " not found");
        }
        if (!book.isAvailable()) {                                               // D5: copy available?
            throw new BusinessRuleException("BookNotAvailable", "No copy of '" + book.getTitle() + "' is available");
        }

        book.decreaseCopies();
        bookRepository.save(book);

        LocalDate today = LocalDate.now(clock);
        BorrowingRecord record = borrowingRepository.save(BorrowingRecord.builder()
                .member(member)
                .book(book)
                .borrowDate(today)
                .dueDate(today.plusDays(rules.getLoanPeriodDays()))
                .build());

        reservationService.fulfillIfReady(userId, bookId);
        audit.log("BORROW", "record=" + record.getId() + " member=" + userId + " book=" + bookId + " due=" + record.getDueDate());
        return record;
    }

    /** Use case "Return Book" (UC-18), with the extensions "Calculate Fine" and "Notify Reservation Availability". */
    @Transactional
    public BorrowingRecord returnBook(Long recordId) {
        BorrowingRecord record = borrowingRepository.findById(recordId).orElse(null);
        if (record == null) {                                                    // D1: record exists?
            throw new NotFoundException("RecordNotFound", "Borrowing record " + recordId + " not found");
        }
        if (record.isReturned()) {                                               // D2: already returned?
            throw new BusinessRuleException("BookAlreadyReturned", "This book was already returned");
        }

        LocalDate today = LocalDate.now(clock);
        record.markReturned(today);

        Book book = record.getBook();
        book.increaseCopies();
        bookRepository.save(book);

        long daysLate = record.daysLate(today);
        if (daysLate > 0) {                                                      // D3: returned late? -> fine
            fineService.createFine(record, daysLate, rulesService.getRules().getFinePerDay());
        }
        reservationService.notifyNextWaiting(book);

        BorrowingRecord saved = borrowingRepository.save(record);
        audit.log("RETURN", "record=" + recordId + " member=" + record.getMember().getId() + " daysLate=" + daysLate);
        return saved;
    }

    /** Use case "Renew Loan" (UC-09). A member can renew only his or her own loans (FR-019). */
    @Transactional
    public BorrowingRecord renewLoan(Long recordId, Long memberId) {
        BorrowingRecord record = borrowingRepository.findById(recordId).orElse(null);
        if (record == null || !record.getMember().getId().equals(memberId)) {   // D1: record exists and is owned?
            throw new NotFoundException("RecordNotFound", "Borrowing record " + recordId + " not found");
        }
        if (record.isReturned()) {                                               // D2: already returned?
            throw new BusinessRuleException("BookAlreadyReturned", "This book was already returned");
        }
        if (record.isOverdue(LocalDate.now(clock))) {                            // D3: overdue?
            throw new BusinessRuleException("RenewalNotAllowed", "An overdue loan cannot be renewed");
        }
        LibraryRules rules = rulesService.getRules();
        if (record.getRenewalCount() >= rules.getMaxRenewals()) {                // D4: renewal limit?
            throw new BusinessRuleException("RenewalNotAllowed", "The loan was already renewed " + record.getRenewalCount() + " time(s)");
        }
        if (reservationService.hasWaitingReservation(record.getBook().getId())) { // D5: book reserved?
            throw new BusinessRuleException("RenewalNotAllowed", "Another member has reserved this book");
        }

        record.renew(rules.getLoanPeriodDays());
        audit.log("RENEW", "record=" + recordId + " newDue=" + record.getDueDate());
        return borrowingRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<BorrowingRecord> getActiveBorrowingsByUser(Long userId) {
        return borrowingRepository.findByMemberIdAndReturnDateIsNull(userId);
    }

    @Transactional(readOnly = true)
    public List<BorrowingRecord> getBorrowingHistory(Long userId) {
        return borrowingRepository.findByMemberIdOrderByBorrowDateDescIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<BorrowingRecord> getOverdueBooks() {
        return borrowingRepository.findOverdueBorrowings(LocalDate.now(clock));
    }
}
