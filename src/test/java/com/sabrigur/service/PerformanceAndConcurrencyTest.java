package com.sabrigur.service;

import com.sabrigur.entity.Book;
import com.sabrigur.entity.Member;
import com.sabrigur.exception.BusinessRuleException;
import com.sabrigur.repository.BookRepository;
import com.sabrigur.repository.BorrowingRecordRepository;
import com.sabrigur.repository.MemberRepository;
import com.sabrigur.starter.LibraryManagmentSystemApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the non-functional requirements that can be measured:
 * NFR-001 (search time with 10,000 books), NFR-002 (50 users at the same time) and
 * NFR-006 (the stock never becomes negative, even when users borrow the last copy at the same time).
 * The test fills the database with a lot of data, so the Spring context is discarded afterwards.
 */
@SpringBootTest(classes = LibraryManagmentSystemApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PerformanceAndConcurrencyTest {

    private static final int USERS = 50;

    @Autowired
    private BookService bookService;
    @Autowired
    private LibraryService libraryService;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private BorrowingRecordRepository borrowingRepository;

    @Test
    @DisplayName("NFR-001: search in a catalog of 10,000 books answers within 2 seconds")
    void searchWithTenThousandBooks() {
        List<Book> books = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            books.add(Book.builder().title("Perf Book " + i).author("Author " + (i % 500))
                    .isbn("PERF-" + i).category(i % 2 == 0 ? "Science" : "History")
                    .totalCopies(3).availableCopies(3).build());
        }
        bookRepository.saveAll(books);

        long slowest = 0;
        String[] keywords = {"Perf Book 77", "Author 42", "PERF-9999", "history", "science", "zzz-no-match"};
        for (int round = 0; round < 5; round++) {
            for (String keyword : keywords) {
                long start = System.nanoTime();
                bookService.search(keyword);
                slowest = Math.max(slowest, (System.nanoTime() - start) / 1_000_000);
            }
        }
        System.out.println("NFR-001 slowest search with 10,000 books: " + slowest + " ms");
        assertThat(slowest).isLessThan(2000);
    }

    @Test
    @DisplayName("NFR-002: 50 users search at the same time and all get an answer")
    void fiftyConcurrentSearches() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < USERS; i++) {
            results.add(pool.submit(() -> {
                go.await();
                return bookService.search("clean").size();
            }));
        }
        long start = System.nanoTime();
        go.countDown();
        for (Future<Integer> f : results) {
            assertThat(f.get(10, TimeUnit.SECONDS)).isGreaterThanOrEqualTo(1);
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        pool.shutdown();
        System.out.println("NFR-002 50 concurrent searches finished in " + ms + " ms");
        assertThat(ms).isLessThan(5000);
    }

    @Test
    @DisplayName("NFR-006: 50 members borrow the last copy at the same time, exactly one succeeds")
    void lastCopyIsBorrowedOnlyOnce() throws Exception {
        Book book = bookRepository.save(Book.builder().title("Last Copy").author("Race").isbn("RACE-1")
                .totalCopies(1).availableCopies(1).build());
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < USERS; i++) {
            members.add(Member.builder().firstName("Racer").lastName("No" + i)
                    .email("racer" + i + "@test.com").passwordHash("hash").build());
        }
        members = memberRepository.saveAll(members);

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (Member m : members) {
            futures.add(pool.submit(() -> {
                try {
                    go.await();
                    libraryService.borrowBook(m.getId(), book.getId());
                    success.incrementAndGet();
                } catch (BusinessRuleException e) {
                    refused.incrementAndGet();           // "BookNotAvailable"
                } catch (Exception e) {
                    conflict.incrementAndGet();          // optimistic lock: another user was faster
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        Book after = bookRepository.findById(book.getId()).orElseThrow();
        long loans = borrowingRepository.findAll().stream().filter(r -> r.getBook().getId().equals(book.getId())).count();
        System.out.println("NFR-006 success=" + success + " refused=" + refused + " conflict=" + conflict
                + " availableCopies=" + after.getAvailableCopies());

        assertThat(success.get()).isEqualTo(1);
        assertThat(after.getAvailableCopies()).isZero();           // never negative
        assertThat(loans).isEqualTo(1);                            // only one borrowing record
    }
}
