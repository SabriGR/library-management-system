package com.sabrigur.entity;

import com.sabrigur.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(unique = true, nullable = false)
    private String isbn;

    private String category;

    private int totalCopies;
    private int availableCopies;

    /** Removed books are hidden from the catalog but stay in the borrowing history. */
    @Builder.Default
    private boolean removed = false;

    /** Optimistic locking: two librarians cannot overwrite each other's stock changes (NFR-006). */
    @Version
    private Long version;

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public void decreaseCopies() {
        if (!isAvailable()) {
            throw new BusinessRuleException("BookNotAvailable", "No copy of '" + title + "' is available");
        }
        availableCopies--;
    }

    public void increaseCopies() {
        if (availableCopies >= totalCopies) {
            throw new BusinessRuleException("InventoryError", "Available copies cannot be more than total copies");
        }
        availableCopies++;
    }
}
