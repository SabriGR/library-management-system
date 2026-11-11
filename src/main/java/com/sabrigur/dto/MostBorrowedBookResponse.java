package com.sabrigur.dto;

public record MostBorrowedBookResponse(Long bookId, String title, Long timesBorrowed) {
}
