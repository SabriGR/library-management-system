package com.sabrigur.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sabrigur.entity.BorrowingRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {

    List<BorrowingRecord> findByUserId(Long userId);

    List<BorrowingRecord> findByUserIdAndReturnDateIsNull(Long userId);

    Optional<BorrowingRecord> findByBookIdAndReturnDateIsNull(Long bookId);

    @Query("SELECT br FROM BorrowingRecord br WHERE br.returnDate IS NULL AND br.dueDate < :currentDate")
    List<BorrowingRecord> findOverdueBorrowings(@Param("currentDate") LocalDate currentDate);
}