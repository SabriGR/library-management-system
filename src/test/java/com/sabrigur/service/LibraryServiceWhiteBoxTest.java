package com.sabrigur.service;

import com.sabrigur.entity.Book;
import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.entity.Member;
import com.sabrigur.exception.BusinessRuleException;
import com.sabrigur.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * WHITE BOX TESTS of {@link LibraryService}.
 * The tests are derived from the structure of the code: every decision node and every independent
 * path of the control flow graph is executed (statement, branch and basis path coverage).
 * The decision nodes (D1, D2 ...) are marked in the source code of LibraryService.
 *
 * borrowBook: decisions D1..D5, cyclomatic complexity V(G) = 5 + 1 = 6, so 6 basis paths:
 *   P1: D1=T                                  member not found
 *   P2: D1=F, D2=T                            member inactive
 *   P3: D1=F, D2=F, D3=T                      loan limit reached
 *   P4: D1=F, D2=F, D3=F, D4=T                book not found
 *   P5: D1=F, D2=F, D3=F, D4=F, D5=T          book not available
 *   P6: all decisions false                   success
 *
 * returnBook: decisions D1..D3, V(G) = 4:
 *   P1: D1=T   P2: D1=F, D2=T   P3: D1=F, D2=F, D3=F (on time)   P4: D1=F, D2=F, D3=T (late, fine)
 *
 * renewLoan: D1 is a compound condition (record == null || not owner), so it has 2 conditions.
 *   decisions D1..D5 + 1 extra condition, V(G) = 7.
 */
class LibraryServiceWhiteBoxTest extends LibraryServiceTestBase {

    // ======================================================== borrowBook

    @Test
    @DisplayName("WB-B1 path P1: D1 true -> UserNotFound, nothing else is called")
    void borrow_P1_memberNotFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.borrowBook(1L, 10L)).isInstanceOf(NotFoundException.class);

        verify(rulesService, never()).getRules();
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("WB-B2 path P2: D1 false, D2 true -> UserInactive")
    void borrow_P2_memberInactive() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1, false)));

        assertThatThrownBy(() -> service.borrowBook(1L, 10L))
                .hasFieldOrPropertyWithValue("code", "UserInactive");

        verify(borrowingRepository, never()).countByMemberIdAndReturnDateIsNull(anyLong());
    }

    @Test
    @DisplayName("WB-B3 path P3: D3 true -> LoanLimitReached, the book is not read")
    void borrow_P3_limitReached() {
        givenBorrowSetup(member(1, true), book(10, 3, 3), 5);

        assertThatThrownBy(() -> service.borrowBook(1L, 10L))
                .hasFieldOrPropertyWithValue("code", "LoanLimitReached");

        verify(bookRepository, never()).findByIdAndRemovedFalse(anyLong());
    }

    @Test
    @DisplayName("WB-B4 path P4: D4 true -> BookNotFound")
    void borrow_P4_bookNotFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1, true)));
        when(borrowingRepository.countByMemberIdAndReturnDateIsNull(1L)).thenReturn(0L);
        when(bookRepository.findByIdAndRemovedFalse(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.borrowBook(1L, 10L))
                .hasFieldOrPropertyWithValue("code", "BookNotFound");

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("WB-B5 path P5: D5 true -> BookNotAvailable, no record is saved")
    void borrow_P5_notAvailable() {
        givenBorrowSetup(member(1, true), book(10, 3, 0), 0);

        assertThatThrownBy(() -> service.borrowBook(1L, 10L))
                .hasFieldOrPropertyWithValue("code", "BookNotAvailable");

        verify(borrowingRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("WB-B6 path P6: all decisions false -> book and record are saved, reservation is fulfilled, audit is written")
    void borrow_P6_success() {
        Member m = member(1, true);
        Book b = book(10, 3, 3);
        givenBorrowSetup(m, b, 2);

        BorrowingRecord r = service.borrowBook(1L, 10L);

        assertThat(r.getDueDate()).isEqualTo(TODAY.plusDays(14));
        verify(bookRepository).save(b);
        verify(borrowingRepository).save(r);
        verify(reservationService).fulfillIfReady(1L, 10L);
        verify(audit).log(any(), any());
    }

    // ======================================================== returnBook

    @Test
    @DisplayName("WB-R1 path P1: D1 true -> RecordNotFound")
    void return_P1_recordNotFound() {
        when(borrowingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnBook(100L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("WB-R2 path P2: D2 true -> BookAlreadyReturned, inventory is not changed")
    void return_P2_alreadyReturned() {
        Book b = book(10, 3, 3);
        givenRecord(record(100, member(1, true), b, TODAY.minusDays(5), TODAY.minusDays(7)));

        assertThatThrownBy(() -> service.returnBook(100L))
                .hasFieldOrPropertyWithValue("code", "BookAlreadyReturned");

        assertThat(b.getAvailableCopies()).isEqualTo(3);
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("WB-R3 path P3: D3 false (on time) -> no fine, next reserved member is notified")
    void return_P3_onTime() {
        Book b = book(10, 3, 2);
        givenRecord(record(100, member(1, true), b, TODAY.plusDays(2), null));

        service.returnBook(100L);

        verify(fineService, never()).createFine(any(), anyLong(), any());
        verify(reservationService).notifyNextWaiting(b);
        assertThat(b.getAvailableCopies()).isEqualTo(3);
    }

    @Test
    @DisplayName("WB-R4 path P4: D3 true (late) -> fine is created with the daily rate from the rules")
    void return_P4_late() {
        rules.setFinePerDay(new java.math.BigDecimal("1.25"));
        Book b = book(10, 3, 2);
        BorrowingRecord r = record(100, member(1, true), b, TODAY.minusDays(4), null);
        givenRecord(r);

        service.returnBook(100L);

        verify(fineService).createFine(r, 4L, new java.math.BigDecimal("1.25"));
    }

    // ======================================================== renewLoan

    @Test
    @DisplayName("WB-N1 D1 condition 1: record == null")
    void renew_D1_recordNull() {
        when(borrowingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renewLoan(100L, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("WB-N2 D1 condition 2: record exists but the owner is another member")
    void renew_D1_notOwner() {
        givenRecord(record(100, member(1, true), book(10, 3, 2), TODAY.plusDays(5), null));

        assertThatThrownBy(() -> service.renewLoan(100L, 2L)).isInstanceOf(NotFoundException.class);

        verify(borrowingRepository, never()).save(any());
    }

    @Test
    @DisplayName("WB-N3 D2 true: returned loan cannot be renewed")
    void renew_D2_returned() {
        givenRecord(record(100, member(1, true), book(10, 3, 3), TODAY.plusDays(5), TODAY.minusDays(1)));

        assertThatThrownBy(() -> service.renewLoan(100L, 1L))
                .hasFieldOrPropertyWithValue("code", "BookAlreadyReturned");
    }

    @Test
    @DisplayName("WB-N4 D3 true: overdue loan cannot be renewed")
    void renew_D3_overdue() {
        givenRecord(record(100, member(1, true), book(10, 3, 2), TODAY.minusDays(1), null));

        assertThatThrownBy(() -> service.renewLoan(100L, 1L))
                .hasFieldOrPropertyWithValue("code", "RenewalNotAllowed");
    }

    @Test
    @DisplayName("WB-N5 D4 true: renewal limit reached")
    void renew_D4_limit() {
        BorrowingRecord r = record(100, member(1, true), book(10, 3, 2), TODAY.plusDays(5), null);
        r.setRenewalCount(1);
        givenRecord(r);

        assertThatThrownBy(() -> service.renewLoan(100L, 1L))
                .hasFieldOrPropertyWithValue("code", "RenewalNotAllowed");

        verify(reservationService, never()).hasWaitingReservation(anyLong());
    }

    @Test
    @DisplayName("WB-N6 D5 true: another member waits for the book")
    void renew_D5_reserved() {
        givenRecord(record(100, member(1, true), book(10, 3, 2), TODAY.plusDays(5), null));
        when(reservationService.hasWaitingReservation(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.renewLoan(100L, 1L))
                .hasFieldOrPropertyWithValue("code", "RenewalNotAllowed");
    }

    @Test
    @DisplayName("WB-N7 all decisions false: due date moves by the loan period and the record is saved")
    void renew_success() {
        BorrowingRecord r = record(100, member(1, true), book(10, 3, 2), TODAY.plusDays(5), null);
        givenRecord(r);

        service.renewLoan(100L, 1L);

        assertThat(r.getDueDate()).isEqualTo(TODAY.plusDays(19));
        verify(borrowingRepository).save(r);
    }

    // ======================================================== simple queries (statement coverage)

    @Test
    @DisplayName("WB-Q1 query methods delegate to the repository")
    void queries() {
        service.getActiveBorrowingsByUser(1L);
        service.getBorrowingHistory(1L);
        service.getOverdueBooks();

        verify(borrowingRepository).findByMemberIdAndReturnDateIsNull(1L);
        verify(borrowingRepository).findByMemberIdOrderByBorrowDateDescIdDesc(1L);
        verify(borrowingRepository).findOverdueBorrowings(LocalDate.of(2026, 3, 10));
    }
}
