package com.sabrigur.service;

import com.sabrigur.entity.Book;
import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.entity.LibraryRules;
import com.sabrigur.entity.Member;
import com.sabrigur.repository.BookRepository;
import com.sabrigur.repository.BorrowingRecordRepository;
import com.sabrigur.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Common set-up for the unit tests of {@link LibraryService}.
 * All repositories and helper services are mocks. The clock is fixed to 10 March 2026,
 * so the due dates and fines can be calculated exactly.
 */
abstract class LibraryServiceTestBase {

    static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    BookRepository bookRepository;
    MemberRepository memberRepository;
    BorrowingRecordRepository borrowingRepository;
    RulesService rulesService;
    FineService fineService;
    ReservationService reservationService;
    AuditService audit;

    LibraryRules rules;
    LibraryService service;

    @BeforeEach
    void setUpBase() {
        bookRepository = mock(BookRepository.class);
        memberRepository = mock(MemberRepository.class);
        borrowingRepository = mock(BorrowingRecordRepository.class);
        rulesService = mock(RulesService.class);
        fineService = mock(FineService.class);
        reservationService = mock(ReservationService.class);
        audit = mock(AuditService.class);

        // default rules: 14 days, 5 loans, 1 renewal, 0.50 per day
        rules = LibraryRules.builder().build();
        when(rulesService.getRules()).thenReturn(rules);
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(borrowingRepository.save(any(BorrowingRecord.class))).thenAnswer(i -> i.getArgument(0));

        ZoneId zone = ZoneId.systemDefault();
        Clock clock = Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone);
        service = new LibraryService(bookRepository, memberRepository, borrowingRepository,
                rulesService, fineService, reservationService, audit, clock);
    }

    // ---------- test data helpers ----------

    Member member(long id, boolean active) {
        Member m = Member.builder().firstName("Test").lastName("Member" + id)
                .email("member" + id + "@test.com").passwordHash("hash").active(active).build();
        m.setId(id);
        return m;
    }

    Book book(long id, int total, int available) {
        return Book.builder().id(id).title("Book " + id).author("Author").isbn("ISBN-" + id)
                .totalCopies(total).availableCopies(available).build();
    }

    BorrowingRecord record(long id, Member member, Book book, LocalDate due, LocalDate returned) {
        return BorrowingRecord.builder().id(id).member(member).book(book)
                .borrowDate(due.minusDays(14)).dueDate(due).returnDate(returned).build();
    }

    /** Prepares an active member, an existing book and a number of active loans. */
    void givenBorrowSetup(Member member, Book book, long activeLoans) {
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(bookRepository.findByIdAndRemovedFalse(book.getId())).thenReturn(Optional.of(book));
        when(borrowingRepository.countByMemberIdAndReturnDateIsNull(member.getId())).thenReturn(activeLoans);
    }

    void givenRecord(BorrowingRecord record) {
        when(borrowingRepository.findById(record.getId())).thenReturn(Optional.of(record));
    }
}
