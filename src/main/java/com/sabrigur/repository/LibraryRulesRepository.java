package com.sabrigur.repository;

import com.sabrigur.entity.LibraryRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LibraryRulesRepository extends JpaRepository<LibraryRules, Long> {
}
