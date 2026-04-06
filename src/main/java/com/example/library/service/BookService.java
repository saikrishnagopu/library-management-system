package com.example.library.service;

import com.example.library.domain.AvailabilityStatus;
import com.example.library.domain.Book;
import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;
import com.example.library.exception.DuplicateIsbnException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BookSpecification;
import com.example.library.util.IsbnUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AsyncEventEnqueueService asyncEventEnqueueService;

    public BookService(BookRepository bookRepository, AsyncEventEnqueueService asyncEventEnqueueService) {
        this.bookRepository = bookRepository;
        this.asyncEventEnqueueService = asyncEventEnqueueService;
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        String normalized = IsbnUtil.normalize(request.getIsbn());
        if (bookRepository.existsByIsbn(normalized)) {
            throw new DuplicateIsbnException("A book with this ISBN already exists.");
        }
        Book book = new Book();
        BookMapper.apply(book, request);
        book.setIsbn(normalized);
        book = bookRepository.save(book);
        return BookMapper.toResponse(book);
    }

    @Transactional(readOnly = true)
    public BookResponse getById(Long id) {
        Book book =
                bookRepository
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
        return BookMapper.toResponse(book);
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> list(String author, Integer publishedYear, Pageable pageable) {
        Specification<Book> spec = BookSpecification.withOptionalFilters(author, publishedYear);
        return bookRepository.findAll(spec, pageable).map(BookMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return Page.empty(pageable);
        }
        return bookRepository.searchByTitleOrAuthor(query.trim(), pageable).map(BookMapper::toResponse);
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book =
                bookRepository
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
        AvailabilityStatus previous = book.getAvailabilityStatus();
        String normalized = IsbnUtil.normalize(request.getIsbn());
        if (!normalized.equals(book.getIsbn()) && bookRepository.existsByIsbn(normalized)) {
            throw new DuplicateIsbnException("A book with this ISBN already exists.");
        }
        BookMapper.apply(book, request);
        book.setIsbn(normalized);
        book = bookRepository.save(book);
        if (previous == AvailabilityStatus.BORROWED && book.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE) {
            asyncEventEnqueueService.enqueueBookAvailabilityChange(book.getId(), AvailabilityStatus.AVAILABLE);
        }
        return BookMapper.toResponse(book);
    }

    /** Soft delete: book stays in DB (ISBN still reserved) but is hidden from list/get/search. */
    @Transactional
    public void softDelete(Long id) {
        Book book =
                bookRepository
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
        book.setDeleted(true);
        bookRepository.save(book);
    }

    @Transactional
    public BookResponse restore(Long id) {
        Book book =
                bookRepository
                        .findByIdAndDeletedTrue(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Deleted book not found: " + id));
        book.setDeleted(false);
        return BookMapper.toResponse(bookRepository.save(book));
    }
}
