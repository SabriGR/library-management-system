package com.sabrigur.repository;

import com.sabrigur.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE :keyword = '' "
            + "OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "ORDER BY m.lastName, m.firstName")
    List<Member> search(@Param("keyword") String keyword);
}
