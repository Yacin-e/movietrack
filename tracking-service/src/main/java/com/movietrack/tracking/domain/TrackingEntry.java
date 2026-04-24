package com.movietrack.tracking.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class TrackingEntry {

    public enum Status { TO_WATCH, WATCHING, WATCHED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long movieId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Integer rating;

    private String notes;

    private LocalDate watchedDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getWatchedDate() { return watchedDate; }
    public void setWatchedDate(LocalDate watchedDate) { this.watchedDate = watchedDate; }
}
