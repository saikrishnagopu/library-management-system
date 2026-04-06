package com.example.library.service;

import com.example.library.domain.Book;
import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;

public final class BookMapper {

    private BookMapper() {}

    public static BookResponse toResponse(Book book) {
        BookResponse r = new BookResponse();
        r.setId(book.getId());
        r.setTitle(book.getTitle());
        r.setAuthor(book.getAuthor());
        r.setIsbn(book.getIsbn());
        r.setPublishedYear(book.getPublishedYear());
        r.setAvailabilityStatus(book.getAvailabilityStatus());
        return r;
    }

    public static void apply(Book book, BookRequest request) {
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublishedYear(request.getPublishedYear());
        book.setAvailabilityStatus(request.getAvailabilityStatus());
    }
}
