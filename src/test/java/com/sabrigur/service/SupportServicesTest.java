package com.sabrigur.service;

import com.sabrigur.dto.BookRequest;
import com.sabrigur.dto.RegisterRequest;
import com.sabrigur.dto.StaffRequest;
import com.sabrigur.entity.*;
import com.sabrigur.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests of the book, member, staff and reservation services (business rules of the requirements). */
class SupportServicesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private final AuditService audit = mock(AuditService.class);

    private BookRepository bookRepository;
    private BorrowingRecordRepository borrowingRepository;
    private MemberRepository memberRepository;
    private UserRepository userRepository;
    private ReservationRepository reservationRepository;
    private NotificationService notificationService;
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        borrowingRepository = mock(BorrowingRecordRepository.class);
        memberRepository = mock(MemberRepository.class);
        userRepository = mock(UserRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        notificationService = mock(NotificationService.class);
        encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenAnswer(i -> "hashed:" + i.getArgument(0));
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(memberRepository.save(any(Member.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ------------------------------------------------------------ BookService

    @Test
    @DisplayName("FR-004 add book: available copies start equal to total copies")
    void book_create() {
        BookService s = new BookService(bookRepository, borrowingRepository, audit);
        when(bookRepository.existsByIsbn("111")).thenReturn(false);

        Book b = s.create(new BookRequest("Title", "Author", "111", "Cat", 4));

        assertThat(b.getTotalCopies()).isEqualTo(4);
        assertThat(b.getAvailableCopies()).isEqualTo(4);
    }

    @Test
    @DisplayName("FR-004 add book: duplicate ISBN is refused")
    void book_createDuplicate() {
        BookService s = new BookService(bookRepository, borrowingRepository, audit);
        when(bookRepository.existsByIsbn("111")).thenReturn(true);

        assertThatThrownBy(() -> s.create(new BookRequest("T", "A", "111", null, 1)))
                .hasFieldOrPropertyWithValue("code", "DuplicateIsbn");
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-005 update book: total copies cannot be less than the copies on loan (boundary 2 on loan)")
    void book_updateCopiesBoundary() {
        BookService s = new BookService(bookRepository, borrowingRepository, audit);
        Book b = Book.builder().id(1L).title("T").author("A").isbn("111").totalCopies(5).availableCopies(3).build();
        when(bookRepository.findByIdAndRemovedFalse(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> s.update(1L, new BookRequest("T", "A", "111", null, 1)))
                .hasFieldOrPropertyWithValue("code", "InvalidCopies");

        Book updated = s.update(1L, new BookRequest("T2", "A", "111", null, 2));   // exactly the copies on loan
        assertThat(updated.getTotalCopies()).isEqualTo(2);
        assertThat(updated.getAvailableCopies()).isZero();
    }

    @Test
    @DisplayName("FR-006 remove book: refused while a copy is on loan, otherwise the book is hidden")
    void book_remove() {
        BookService s = new BookService(bookRepository, borrowingRepository, audit);
        Book b = Book.builder().id(1L).title("T").author("A").isbn("111").totalCopies(1).availableCopies(1).build();
        when(bookRepository.findByIdAndRemovedFalse(1L)).thenReturn(Optional.of(b));
        when(borrowingRepository.existsByBookIdAndReturnDateIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> s.remove(1L)).hasFieldOrPropertyWithValue("code", "BookOnLoan");
        assertThat(b.isRemoved()).isFalse();

        when(borrowingRepository.existsByBookIdAndReturnDateIsNull(1L)).thenReturn(false);
        s.remove(1L);
        assertThat(b.isRemoved()).isTrue();
    }

    @Test
    @DisplayName("FR-007 search: a null keyword searches all books")
    void book_searchNullKeyword() {
        new BookService(bookRepository, borrowingRepository, audit).search(null);
        verify(bookRepository).search("");
    }

    // ------------------------------------------------------------ MemberService

    @Test
    @DisplayName("FR-001 register: the password is stored as a hash and the e-mail in lower case (NFR-003)")
    void member_register() {
        MemberService s = new MemberService(memberRepository, userRepository, borrowingRepository, mock(FineService.class), encoder, audit);

        Member m = s.register(new RegisterRequest("Ann", "Lee", " Ann@Test.COM ", "555", "secret123"));

        assertThat(m.getEmail()).isEqualTo("ann@test.com");
        assertThat(m.getPasswordHash()).isEqualTo("hashed:secret123").isNotEqualTo("secret123");
        assertThat(m.isActive()).isTrue();
    }

    @Test
    @DisplayName("FR-001 register: an e-mail address can be used only once")
    void member_registerDuplicate() {
        MemberService s = new MemberService(memberRepository, userRepository, borrowingRepository, mock(FineService.class), encoder, audit);
        when(userRepository.existsByEmail("ann@test.com")).thenReturn(true);

        assertThatThrownBy(() -> s.register(new RegisterRequest("Ann", "Lee", "ann@test.com", null, "secret123")))
                .hasFieldOrPropertyWithValue("code", "EmailAlreadyUsed");
    }

    @Test
    @DisplayName("FR-011 deactivate member")
    void member_deactivate() {
        MemberService s = new MemberService(memberRepository, userRepository, borrowingRepository, mock(FineService.class), encoder, audit);
        Member m = Member.builder().firstName("A").lastName("B").email("a@b.c").passwordHash("h").build();
        m.setId(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThat(s.setActive(1L, false).isActive()).isFalse();
    }

    // ------------------------------------------------------------ StaffService

    @Test
    @DisplayName("FR-029 staff: an administrator cannot deactivate his or her own account")
    void staff_cannotDeactivateSelf() {
        LibrarianRepository librarians = mock(LibrarianRepository.class);
        StaffService s = new StaffService(librarians, userRepository, encoder, audit);
        Librarian admin = Administrator.builder().firstName("A").lastName("B").email("admin@library.com").passwordHash("h").build();
        when(librarians.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> s.setActive(1L, false, "ADMIN@library.com"))
                .hasFieldOrPropertyWithValue("code", "CannotDeactivateSelf");
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    @DisplayName("FR-029 staff: a new librarian gets a hashed password")
    void staff_create() {
        LibrarianRepository librarians = mock(LibrarianRepository.class);
        when(librarians.save(any(Librarian.class))).thenAnswer(i -> i.getArgument(0));
        StaffService s = new StaffService(librarians, userRepository, encoder, audit);

        Librarian l = s.create(new StaffRequest("L", "M", "l@m.com", null, "password1", "E-1"));

        assertThat(l.getPasswordHash()).isEqualTo("hashed:password1");
        assertThat(l.getRole()).isEqualTo("LIBRARIAN");
    }

    // ------------------------------------------------------------ ReservationService

    private ReservationService reservationService() {
        return new ReservationService(reservationRepository, memberRepository, bookRepository, notificationService, audit, clock);
    }

    private Member activeMember() {
        Member m = Member.builder().firstName("A").lastName("B").email("a@b.c").passwordHash("h").build();
        m.setId(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(m));
        return m;
    }

    @Test
    @DisplayName("FR-023 reserve: refused when a copy is available")
    void reserve_bookAvailable() {
        activeMember();
        when(bookRepository.findByIdAndRemovedFalse(5L))
                .thenReturn(Optional.of(Book.builder().id(5L).title("T").totalCopies(2).availableCopies(1).build()));

        assertThatThrownBy(() -> reservationService().reserve(1L, 5L))
                .hasFieldOrPropertyWithValue("code", "BookAvailable");
    }

    @Test
    @DisplayName("FR-023 reserve: refused when the member already has an active reservation")
    void reserve_duplicate() {
        activeMember();
        when(bookRepository.findByIdAndRemovedFalse(5L))
                .thenReturn(Optional.of(Book.builder().id(5L).title("T").totalCopies(2).availableCopies(0).build()));
        when(reservationRepository.existsByMemberIdAndBookIdAndStatusIn(eq(1L), eq(5L), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> reservationService().reserve(1L, 5L))
                .hasFieldOrPropertyWithValue("code", "AlreadyReserved");
    }

    @Test
    @DisplayName("FR-023 reserve: success when no copy is available")
    void reserve_success() {
        activeMember();
        when(bookRepository.findByIdAndRemovedFalse(5L))
                .thenReturn(Optional.of(Book.builder().id(5L).title("T").totalCopies(2).availableCopies(0).build()));

        Reservation r = reservationService().reserve(1L, 5L);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.WAITING);
        assertThat(r.getReservationDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("FR-024 cancel: a member can cancel only his or her own reservation")
    void cancel_ownership() {
        Member owner = activeMember();
        Reservation r = Reservation.builder().id(7L).member(owner).reservationDate(TODAY).build();
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> reservationService().cancel(7L, 2L)).hasFieldOrPropertyWithValue("code", "ReservationNotFound");
        assertThat(reservationService().cancel(7L, 1L).getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("FR-025 notify: the first waiting member gets an e-mail and the reservation becomes READY")
    void notifyNextWaiting() {
        Member m = activeMember();
        Book b = Book.builder().id(5L).title("Clean Code").totalCopies(2).availableCopies(1).build();
        Reservation r = Reservation.builder().member(m).book(b).reservationDate(TODAY).build();
        when(reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAscIdAsc(5L, ReservationStatus.WAITING))
                .thenReturn(Optional.of(r));

        reservationService().notifyNextWaiting(b);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.READY);
        verify(notificationService).sendEmail(eq("a@b.c"), any(), any());
    }

    @Test
    @DisplayName("FR-025 notify: nothing happens when nobody waits")
    void notifyNextWaiting_nobody() {
        Book b = Book.builder().id(5L).title("T").totalCopies(2).availableCopies(1).build();
        when(reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAscIdAsc(anyLong(), any()))
                .thenReturn(Optional.empty());

        assertThat(reservationService().notifyNextWaiting(b)).isEmpty();
        verify(notificationService, never()).sendEmail(any(), any(), any());
    }
}
