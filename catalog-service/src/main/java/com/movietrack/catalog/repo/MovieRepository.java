package com.movietrack.catalog.repo;

import com.movietrack.catalog.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
