package com.movietrack.tracking.repo;

import com.movietrack.tracking.domain.TrackingEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingRepository extends JpaRepository<TrackingEntry, Long> {
}
