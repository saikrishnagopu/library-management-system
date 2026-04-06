package com.example.library.repository;

import com.example.library.domain.Book;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    boolean existsByIsbn(String isbn);

    Optional<Book> findByIdAndDeletedFalse(Long id);

    Optional<Book> findByIdAndDeletedTrue(Long id);

    @Query(
            """
            SELECT b FROM Book b
            WHERE b.deleted = false
              AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Book> searchByTitleOrAuthor(@Param("q") String q, Pageable pageable);
}
