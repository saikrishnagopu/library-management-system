package com.example.library.service;

import com.example.library.domain.Book;
import com.example.library.domain.User;
import com.example.library.domain.WishlistEntry;
import com.example.library.dto.WishlistRequest;
import com.example.library.exception.ConflictException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.UserRepository;
import com.example.library.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public WishlistService(
            WishlistRepository wishlistRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public void add(WishlistRequest request) {
        User user = userRepository
                .findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));
        Book book =
                bookRepository
                        .findByIdAndDeletedFalse(request.getBookId())
                        .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + request.getBookId()));
        if (wishlistRepository.existsByUserIdAndBookId(user.getId(), book.getId())) {
            throw new ConflictException("Book is already on this user's wishlist.");
        }
        WishlistEntry entry = new WishlistEntry();
        entry.setUserId(user.getId());
        entry.setBookId(book.getId());
        wishlistRepository.save(entry);
    }
}
