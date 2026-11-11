package com.sabrigur.config;

import com.sabrigur.entity.*;
import com.sabrigur.repository.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Creates demo data on an empty database: library rules, an administrator, a librarian, members, books,
 * borrowing records (active, overdue, returned late with a fine) and one reservation.
 * The passwords below are for demonstration only.
 */
@Configuration
public class DataSeeder {

    public static final String ADMIN_EMAIL = "admin@library.com";
    public static final String ADMIN_PASSWORD = "Admin123!";
    public static final String LIBRARIAN_EMAIL = "librarian@library.com";
    public static final String LIBRARIAN_PASSWORD = "Librarian123!";
    public static final String MEMBER_PASSWORD = "Member123!";

    @Bean
    public CommandLineRunner initData(BookRepository bookRepository,
                                      UserRepository userRepository,
                                      BorrowingRecordRepository borrowingRecordRepository,
                                      ReservationRepository reservationRepository,
                                      LibraryRulesRepository rulesRepository,
                                      PasswordEncoder encoder) {
        return args -> {
            if (bookRepository.count() != 0 || userRepository.count() != 0) {
                return;
            }

            rulesRepository.save(LibraryRules.builder().build());

            // Staff
            userRepository.save(Administrator.builder().firstName("System").lastName("Administrator")
                    .email(ADMIN_EMAIL).passwordHash(encoder.encode(ADMIN_PASSWORD)).employeeNo("A-001").build());
            userRepository.save(Librarian.builder().firstName("Lara").lastName("Librarian")
                    .email(LIBRARIAN_EMAIL).passwordHash(encoder.encode(LIBRARIAN_PASSWORD)).employeeNo("L-001").build());

            // Members
            String memberHash = encoder.encode(MEMBER_PASSWORD);
            Member user1 = userRepository.save(Member.builder().firstName("Sabri").lastName("Gür")
                    .email("sabri@library.com").phoneNumber("5551112233").passwordHash(memberHash).build());
            Member user2 = userRepository.save(Member.builder().firstName("Bahri").lastName("Ayzabar")
                    .email("bahri@library.com").phoneNumber("5554445566").passwordHash(memberHash).build());
            Member user3 = userRepository.save(Member.builder().firstName("Oğuzhan").lastName("Işık")
                    .email("oguzhan@library.com").phoneNumber("5557778899").passwordHash(memberHash).build());

            // Books
            Book book1 = Book.builder().title("Spring Boot in Action").author("Craig Walls").isbn("9781617292545").category("Programming").totalCopies(5).availableCopies(4).build();
            Book book2 = Book.builder().title("Clean Code").author("Robert C. Martin").isbn("9780132350884").category("Programming").totalCopies(3).availableCopies(3).build();
            Book book3 = Book.builder().title("Computer Organization and Design").author("David A. Patterson").isbn("9780124077263").category("Computer Science").totalCopies(2).availableCopies(1).build();
            Book book4 = Book.builder().title("Türkiye Coğrafyası").author("Ramazan Özey").isbn("9786053181234").category("Geography").totalCopies(4).availableCopies(4).build();
            Book book5 = Book.builder().title("English Grammar in Use").author("Raymond Murphy").isbn("9781108457651").category("Language").totalCopies(6).availableCopies(6).build();
            Book book6 = Book.builder().title("Design Patterns").author("Erich Gamma").isbn("9780201633610").category("Programming").totalCopies(1).availableCopies(0).build();
            bookRepository.saveAll(List.of(book1, book2, book3, book4, book5, book6));

            LocalDate today = LocalDate.now();

            // Active loan, not yet due
            BorrowingRecord record1 = BorrowingRecord.builder().member(user1).book(book1)
                    .borrowDate(today.minusDays(5)).dueDate(today.plusDays(9)).build();
            // Active loan, overdue
            BorrowingRecord record2 = BorrowingRecord.builder().member(user2).book(book3)
                    .borrowDate(today.minusDays(20)).dueDate(today.minusDays(6)).build();
            // Returned on time
            BorrowingRecord record3 = BorrowingRecord.builder().member(user3).book(book5)
                    .borrowDate(today.minusDays(30)).dueDate(today.minusDays(16)).returnDate(today.minusDays(18)).build();
            // The only copy of "Design Patterns" is on loan (user3 reserves it below)
            BorrowingRecord record4 = BorrowingRecord.builder().member(user2).book(book6)
                    .borrowDate(today.minusDays(4)).dueDate(today.plusDays(10)).build();
            // Returned 6 days late, with an unpaid fine
            BorrowingRecord record5 = BorrowingRecord.builder().member(user1).book(book2)
                    .borrowDate(today.minusDays(40)).dueDate(today.minusDays(26)).returnDate(today.minusDays(20)).build();
            Fine fine = Fine.builder().borrowingRecord(record5)
                    .amount(Fine.calculateAmount(6, new BigDecimal("0.50"))).build();
            record5.setFine(fine);
            borrowingRecordRepository.saveAll(List.of(record1, record2, record3, record4, record5));

            reservationRepository.save(Reservation.builder().member(user3).book(book6).reservationDate(today.minusDays(1)).build());
        };
    }
}
