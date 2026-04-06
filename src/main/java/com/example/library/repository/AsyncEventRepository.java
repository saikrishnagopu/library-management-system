package com.example.library.repository;

import com.example.library.domain.AsyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsyncEventRepository extends JpaRepository<AsyncEvent, Long> {}
