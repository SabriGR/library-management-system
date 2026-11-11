package com.sabrigur.service;

import com.sabrigur.entity.Book;
import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.entity.Member;
import com.sabrigur.exception.BusinessRuleException;
import com.sabrigur.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BLACK BOX TESTS of the circulation functions.
 * The tests are derived only from the requirements (FR-013 to FR-019), not from the code.
 * Techniques: equivalence partitioning and boundary value analysis.
 *
 * Input partitions of "Borrow Book":
 *   member    : valid active | not found | inactive
 *   loans     : 0..limit-1 (valid) | limit and more (invalid), boundary values 0, 1, 4, 5, 6
 *   book      : valid with copies | not found | no copies, boundary values 0, 1, 2 copies
 */
class LibraryServiceBlackBoxTest extends LibraryServiceTestBase {

    // ---------------------------------------------------------------- Borrow Book

    @Test
    @DisplayName("BB-01 valid member, available book: loan is created, due date = today + 14 days, copies - 1")
    void borrow_validInput_success() {
        Member m = member(1, true);
        Book b = book(10, 3, 3);
        givenBorrowSetup(m, b, 0);

        BorrowingRecord r = service.borrowBook(1L, 10L);

        assertThat(r.getMember()).isSameAs(m);
        assertThat(r.getBook()).isSameAs(b);
        assertThat(r.getBorrowDate()).isEqualTo(TODAY);
        assertThat(r.getDueDate()).isEqualTo(TODAY.plusDays(14));
        assertThat(r.getReturnDate()).isNull();
        assertThat(b.getAvailableCopies()).isEqualTo(2);
    }

    @ParameterizedTest(name = "BB-02 {0} active loans -> borrowing allowed = {1}")
    @CsvSource({"0,true", "1,true", "4,true", "5,false", "6,false"})
    @DisplayName("BB-02 boundary values of the loan limit (limit = 5)")
    void borrow_loanLimitBoundaries(long activeLoans, boolean allowed) {
        Member m = member(1, true);
        Book b = book(10, 10, 10);
        givenBorrowSetup(m, b, activeLoans);

        if (allowed) {
            assertThat(service.borrowBook(1L, 10L)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.borrowBook(1L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("code", "LoanLimitReached");
            assertThat(b.getAvailableCopies()).isEqualTo(10);       // nothing changed
        }
    }

    @ParameterizedTest(name = "BB-03 {0} available copies -> borrowing allowed = {1}")
    @CsvSource({"0,false", "1,true", "2,true"})
    @DisplayName("BB-03 boundary values of the available copies")
    void borrow_availableCopiesBoundaries(int copies, boolean allowed) {
        Member m = member(1, true);
        Book b = book(10, 5, copies);
        givenBorrowSetup(m, b, 0);

        if (allowed) {
            service.borrowBook(1L, 10L);
            assertThat(b.getAvailableCopies()).isEqualTo(copies - 1);
        } else {
            assertThatThrownBy(() -> service.borrowBook(1L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("code", "BookNotAvailable");
            assertThat(b.getAvailableCopies()).isZero();
        }
    }

    @Test
    @DisplayName("BB-04 invalid partition: member does not exist")
    void borrow_unknownMember() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.borrowBook(99L, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "UserNotFound");
    }

    @Test
    @DisplayName("BB-05 invalid partition: member is inactive")
    void borrow_inactiveMember() {
        Member m = member(1, false);
        givenBorrowSetup(m, book(10, 3, 3), 0);

        assertThatThrownBy(() -> service.borrowBook(1L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "UserInactive");
    }

    @Test
    @DisplayName("BB-06 invalid partition: book does not exist")
    void borrow_unknownBook() {
        Member m = member(1, true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(m));
        when(borrowingRepository.countByMemberIdAndReturnDateIsNull(1L)).thenReturn(0L);
        when(bookRepository.findByIdAndRemovedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.borrowBook(1L, 99L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "BookNotFound");
    }

    @Test
    @DisplayName("BB-07 the loan period is configurable (FR-030): 30 days")
    void borrow_usesConfiguredLoanPeriod() {
        rules.setLoanPeriodDays(30);
        givenBorrowSetup(member(1, true), book(10, 3, 3), 0);

        assertThat(service.borrowBook(1L, 10L).getDueDate()).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    @DisplayName("BB-08 the loan limit is configurable (FR-030): limit 2")
    void borrow_usesConfiguredLoanLimit() {
        rules.setMaxLoansPerMember(2);
        givenBorrowSetup(member(1, true), book(10, 3, 3), 2);

        assertThatThrownBy(() -> service.borrowBook(1L, 10L))
                .hasFieldOrPropertyWithValue("code", "LoanLimitReached");
    }

    // ---------------------------------------------------------------- Return Book

    @ParameterizedTest(name = "BB-09 returned {0} days late -> fine expected = {1}")
    @CsvSource({"-3,false", "-1,false", "0,false", "1,true", "2,true", "10,true"})
    @DisplayName("BB-09 boundary values of the return date (due date = boundary)")
    void return_fineOnlyWhenLate(long daysLate, boolean fineExpected) {
        Member m = member(1, true);
        Book b = book(10, 3, 2);
        BorrowingRecord r = record(100, m, b, TODAY.minusDays(daysLate), null);
        givenRecord(r);

        BorrowingRecord result = service.returnBook(100L);

        assertThat(result.getReturnDate()).isEqualTo(TODAY);
        assertThat(b.getAvailableCopies()).isEqualTo(3);
        if (fineExpected) {
            ArgumentCaptor<Long> days = ArgumentCaptor.forClass(Long.class);
            verify(fineService).createFine(any(BorrowingRecord.class), days.capture(), any());
            assertThat(days.getValue()).isEqualTo(daysLate);
        } else {
            verify(fineService, never()).createFine(any(), anyLong(), any());
        }
    }

    @Test
    @DisplayName("BB-10 invalid partition: record does not exist")
    void return_unknownRecord() {
        when(borrowingRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnBook(5L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "RecordNotFound");
    }

    @Test
    @DisplayName("BB-11 invalid partition: the book was already returned")
    void return_alreadyReturned() {
        BorrowingRecord r = record(100, member(1, true), book(10, 3, 3), TODAY.minusDays(5), TODAY.minusDays(6));
        givenRecord(r);

        assertThatThrownBy(() -> service.returnBook(100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "BookAlreadyReturned");
    }

    // ---------------------------------------------------------------- Renew Loan

    @Test
    @DisplayName("BB-12 renew: valid loan is extended by 14 days")
    void renew_valid() {
        Member m = member(1, true);
        BorrowingRecord r = record(100, m, book(10, 3, 2), TODAY.plusDays(3), null);
        givenRecord(r);

        BorrowingRecord result = service.renewLoan(100L, 1L);

        assertThat(result.getDueDate()).isEqualTo(TODAY.plusDays(17));
        assertThat(result.getRenewalCount()).isEqualTo(1);
    }

    @ParameterizedTest(name = "BB-13 due date is {0} days from today -> renewal allowed = {1}")
    @CsvSource({"3,true", "1,true", "0,true", "-1,false", "-5,false"})
    @DisplayName("BB-13 boundary values of the due date: a loan that is due today can still be renewed")
    void renew_overdueBoundary(long daysUntilDue, boolean allowed) {
        BorrowingRecord r = record(100, member(1, true), book(10, 3, 2), TODAY.plusDays(daysUntilDue), null);
        givenRecord(r);

        if (allowed) {
            assertThat(service.renewLoan(100L, 1L).getRenewalCount()).isEqualTo(1);
        } else {
            assertThatThrownBy(() -> service.renewLoan(100L, 1L))
                    .hasFieldOrPropertyWithValue("code", "RenewalNotAllowed");
        }
    }

    @ParameterizedTest(name = "BB-14 renewal count {0} (limit 1) -> renewal allowed = {1}")
    @CsvSource({"0,true", "1,false", "2,false"})
    @DisplayName("BB-14 boundary values of the renewal limit")
    void renew_renewalLimitBoundary(int renewals, boolean allowed) {
        BorrowingRecord r = record(100, member(1, true), book(10, 3, 2), TODAY.plusDays(5), null);
        r.setRenewalCount(renewals);
        givenRecord(r);

        if (allowed) {
            assertThat(service.renewLoan(100L, 1L)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.renewLoan(100L, 1L))
                    .hasFieldOrPropertyWithValue("code", "RenewalNotAllowed");
        }
    }

    @Test
    @DisplayName("BB-15 renewal is refused when another member reserved the book")
    void renew_refusedWhenReserved() {
        BorrowingRecord r = record(100, member(1, true), book(10, 3, 2), TODAY.plusDays(5), null);
        givenRecord(r);
        when(reservationService.hasWaitingReservation(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.renewLoan(100L, 1L))
                .hasFieldOrPropertyWithValue("code", "RenewalNotAllowed");
    }

    @Test
    @DisplayName("BB-16 a member cannot renew the loan of another member")
    void renew_otherMembersLoan() {
        BorrowingRecord r = record(100, member(1, true), book(10, 3, 2), TODAY.plusDays(5), null);
        givenRecord(r);

        assertThatThrownBy(() -> service.renewLoan(100L, 2L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- Overdue list

    @Test
    @DisplayName("BB-17 overdue query uses today's date")
    void overdue_usesToday() {
        service.getOverdueBooks();

        verify(borrowingRepository).findOverdueBorrowings(LocalDate.of(2026, 3, 10));
    }
}
