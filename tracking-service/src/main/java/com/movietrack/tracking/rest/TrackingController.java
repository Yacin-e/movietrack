package com.movietrack.tracking.rest;

import com.movietrack.grpc.MovieResponse;
import com.movietrack.tracking.domain.TrackingEntry;
import com.movietrack.tracking.grpc.CatalogGrpcClient;
import com.movietrack.tracking.repo.TrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private static final Logger log = LoggerFactory.getLogger(TrackingController.class);

    private final TrackingRepository repo;
    private final CatalogGrpcClient grpcClient;

    public TrackingController(TrackingRepository repo, CatalogGrpcClient grpcClient) {
        this.repo = repo;
        this.grpcClient = grpcClient;
    }

    @GetMapping
    public List<TrackingEntry> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrackingEntry> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TrackingEntry create(@RequestBody TrackingEntry entry) {
        return repo.save(entry);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrackingEntry> update(@PathVariable Long id, @RequestBody TrackingEntry entry) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        entry.setId(id);
        return ResponseEntity.ok(repo.save(entry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/full")
    public List<Map<String, Object>> full() {
        return repo.findAll().stream().map(entry -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", entry.getId());
            result.put("movieId", entry.getMovieId());
            result.put("status", entry.getStatus());
            result.put("rating", entry.getRating());
            result.put("notes", entry.getNotes());
            result.put("watchedDate", entry.getWatchedDate());
            try {
                MovieResponse movie = grpcClient.getMovie(entry.getMovieId());
                result.put("movie", Map.of(
                        "id", movie.getId(),
                        "title", movie.getTitle(),
                        "director", movie.getDirector(),
                        "releaseYear", movie.getReleaseYear(),
                        "genre", movie.getGenre(),
                        "runtimeMinutes", movie.getRuntimeMinutes(),
                        "posterUrl", movie.getPosterUrl()
                ));
            } catch (Exception e) {
                log.error("gRPC call failed for movieId={}", entry.getMovieId(), e);
                result.put("movie", null);
            }
            return result;
        }).toList();
    }
}
