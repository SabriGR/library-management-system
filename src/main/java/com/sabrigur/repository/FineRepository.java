package com.sabrigur.repository;

import com.sabrigur.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByPaidFalse();

    List<Fine> findByBorrowingRecordMemberIdOrderByIdDesc(Long memberId);

    List<Fine> findByBorrowingRecordMemberIdAndPaidFalse(Long memberId);
}
