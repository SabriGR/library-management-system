package com.sabrigur.entity;

import com.sabrigur.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests of the domain classes (white box: every branch of the entity methods). */
class DomainModelTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    // ------------------------------------------------------------ Book

    @Test
    @DisplayName("Book: decreaseCopies reduces the stock by one")
    void book_decrease() {
        Book b = Book.builder().title("T").totalCopies(2).availableCopies(2).build();
        b.decreaseCopies();
        assertThat(b.getAvailableCopies()).isEqualTo(1);
        assertThat(b.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Book: the stock can never become negative (NFR-006)")
    void book_decreaseAtZero() {
        Book b = Book.builder().title("T").totalCopies(2).availableCopies(0).build();
        assertThat(b.isAvailable()).isFalse();
        assertThatThrownBy(b::decreaseCopies).isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "BookNotAvailable");
    }

    @Test
    @DisplayName("Book: the stock can never be larger than the total copies (NFR-006)")
    void book_increaseLimit() {
        Book b = Book.builder().title("T").totalCopies(2).availableCopies(1).build();
        b.increaseCopies();
        assertThat(b.getAvailableCopies()).isEqualTo(2);
        assertThatThrownBy(b::increaseCopies).isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "InventoryError");
    }

    // ------------------------------------------------------------ BorrowingRecord

    @ParameterizedTest(name = "due {0}, today {1}: overdue = {2}")
    @CsvSource({"2026-03-10,2026-03-09,false", "2026-03-10,2026-03-10,false", "2026-03-10,2026-03-11,true"})
    @DisplayName("BorrowingRecord: overdue starts on the day after the due date")
    void record_overdueBoundary(LocalDate due, LocalDate today, boolean expected) {
        BorrowingRecord r = BorrowingRecord.builder().borrowDate(due.minusDays(14)).dueDate(due).build();
        assertThat(r.isOverdue(today)).isEqualTo(expected);
    }

    @Test
    @DisplayName("BorrowingRecord: a returned record is never overdue")
    void record_returnedNotOverdue() {
        BorrowingRecord r = BorrowingRecord.builder().borrowDate(TODAY.minusDays(30)).dueDate(TODAY.minusDays(16))
                .returnDate(TODAY.minusDays(1)).build();
        assertThat(r.isReturned()).isTrue();
        assertThat(r.isOverdue(TODAY)).isFalse();
    }

    @ParameterizedTest(name = "return {0} days after the due date -> {1} late days")
    @CsvSource({"-5,0", "0,0", "1,1", "9,9"})
    @DisplayName("BorrowingRecord: late days are never negative")
    void record_daysLate(long offset, long expected) {
        BorrowingRecord r = BorrowingRecord.builder().borrowDate(TODAY.minusDays(14)).dueDate(TODAY).build();
        assertThat(r.daysLate(TODAY.plusDays(offset))).isEqualTo(expected);
    }

    @Test
    @DisplayName("BorrowingRecord: renew moves the due date and counts the renewal")
    void record_renew() {
        BorrowingRecord r = BorrowingRecord.builder().borrowDate(TODAY).dueDate(TODAY.plusDays(14)).build();
        r.renew(14);
        assertThat(r.getDueDate()).isEqualTo(TODAY.plusDays(28));
        assertThat(r.getRenewalCount()).isEqualTo(1);
    }

    // ------------------------------------------------------------ Fine

    @ParameterizedTest(name = "{0} late days x {1} per day = {2}")
    @CsvSource({"0,0.50,0.00", "1,0.50,0.50", "2,0.50,1.00", "30,0.50,15.00", "7,0.00,0.00", "3,0.333,1.00", "1,0.005,0.01"})
    @DisplayName("Fine: amount = late days x daily rate, rounded to 2 decimals (FR-027)")
    void fine_amount(long days, String rate, String expected) {
        assertThat(Fine.calculateAmount(days, new BigDecimal(rate))).isEqualByComparingTo(expected);
        assertThat(Fine.calculateAmount(days, new BigDecimal(rate)).scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Fine: payment stores the date, a second payment is refused (FR-028)")
    void fine_pay() {
        Fine f = Fine.builder().amount(new BigDecimal("3.00")).build();
        f.pay(TODAY);
        assertThat(f.isPaid()).isTrue();
        assertThat(f.getPaymentDate()).isEqualTo(TODAY);
        assertThatThrownBy(() -> f.pay(TODAY)).hasFieldOrPropertyWithValue("code", "FineAlreadyPaid");
    }

    // ------------------------------------------------------------ Reservation

    @Test
    @DisplayName("Reservation: life cycle WAITING -> READY -> FULFILLED")
    void reservation_lifeCycle() {
        Reservation r = Reservation.builder().reservationDate(TODAY).build();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.WAITING);
        assertThat(r.isActive()).isTrue();
        r.markReady();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.READY);
        r.fulfill();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
        assertThat(r.isActive()).isFalse();
    }

    @Test
    @DisplayName("Reservation: cancel works once, a cancelled reservation cannot be cancelled again")
    void reservation_cancel() {
        Reservation r = Reservation.builder().reservationDate(TODAY).build();
        r.cancel();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThatThrownBy(r::cancel).hasFieldOrPropertyWithValue("code", "ReservationNotActive");
    }

    // ------------------------------------------------------------ User roles

    @Test
    @DisplayName("User hierarchy: roles of member, librarian and administrator")
    void user_roles() {
        assertThat(Member.builder().build().getRole()).isEqualTo("MEMBER");
        assertThat(Librarian.builder().build().getRole()).isEqualTo("LIBRARIAN");
        Administrator admin = Administrator.builder().firstName("A").lastName("B").build();
        assertThat(admin.getRole()).isEqualTo("ADMIN");
        assertThat(admin).isInstanceOf(Librarian.class);
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getFullName()).isEqualTo("A B");
    }
}
