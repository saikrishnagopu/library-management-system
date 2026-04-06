package com.example.library.repository;

import com.example.library.domain.WishlistEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistRepository extends JpaRepository<WishlistEntry, Long> {

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT w.userId FROM WishlistEntry w WHERE w.bookId = :bookId ORDER BY w.id")
    Page<Long> findUserIdsByBookId(@Param("bookId") Long bookId, Pageable pageable);
}
