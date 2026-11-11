package com.sabrigur.repository;

import com.sabrigur.entity.Librarian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Returns librarians and administrators (an administrator is a librarian). */
@Repository
public interface LibrarianRepository extends JpaRepository<Librarian, Long> {
}
