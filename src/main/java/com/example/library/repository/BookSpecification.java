package com.example.library.repository;

import com.example.library.domain.Book;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecification {

    private BookSpecification() {}

    public static Specification<Book> withOptionalFilters(String author, Integer publishedYear) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (author != null && !author.isBlank()) {
                predicates.add(
                        cb.like(cb.lower(root.get("author")), "%" + author.trim().toLowerCase() + "%"));
            }
            if (publishedYear != null) {
                predicates.add(cb.equal(root.get("publishedYear"), publishedYear));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
