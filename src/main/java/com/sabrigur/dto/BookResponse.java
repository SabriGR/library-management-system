package com.sabrigur.dto;

import com.sabrigur.entity.Book;

public record BookResponse(Long id, String title, String author, String isbn, String category,
                           int totalCopies, int availableCopies) {

    public static BookResponse from(Book b) {
        return new BookResponse(b.getId(), b.getTitle(), b.getAuthor(), b.getIsbn(), b.getCategory(),
                b.getTotalCopies(), b.getAvailableCopies());
    }
}
