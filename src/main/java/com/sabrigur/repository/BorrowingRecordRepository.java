package com.sabrigur.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sabrigur.dto.MostBorrowedBookResponse;
import com.sabrigur.entity.BorrowingRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {

    List<BorrowingRecord> findByMemberIdOrderByBorrowDateDescIdDesc(Long memberId);

    List<BorrowingRecord> findByMemberIdAndReturnDateIsNull(Long memberId);

    long countByMemberIdAndReturnDateIsNull(Long memberId);

    Optional<BorrowingRecord> findByBookIdAndReturnDateIsNull(Long bookId);

    boolean existsByBookIdAndReturnDateIsNull(Long bookId);

    @Query("SELECT br FROM BorrowingRecord br WHERE br.returnDate IS NULL AND br.dueDate < :currentDate ORDER BY br.dueDate ASC")
    List<BorrowingRecord> findOverdueBorrowings(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT new com.sabrigur.dto.MostBorrowedBookResponse(br.book.id, br.book.title, COUNT(br)) "
            + "FROM BorrowingRecord br GROUP BY br.book.id, br.book.title ORDER BY COUNT(br) DESC, br.book.title ASC")
    List<MostBorrowedBookResponse> findMostBorrowed(Pageable pageable);
}
