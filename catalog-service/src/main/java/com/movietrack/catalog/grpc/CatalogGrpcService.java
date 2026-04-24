package com.movietrack.catalog.grpc;

import com.movietrack.catalog.repo.MovieRepository;
import com.movietrack.grpc.CatalogServiceGrpc;
import com.movietrack.grpc.MovieRequest;
import com.movietrack.grpc.MovieResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {

    private final MovieRepository repo;

    public CatalogGrpcService(MovieRepository repo) {
        this.repo = repo;
    }

    @Override
    public void getMovie(MovieRequest request, StreamObserver<MovieResponse> responseObserver) {
        repo.findById(request.getId()).ifPresent(movie ->
            responseObserver.onNext(MovieResponse.newBuilder()
                .setId(movie.getId())
                .setTitle(movie.getTitle())
                .setDirector(movie.getDirector())
                .setReleaseYear(movie.getReleaseYear())
                .setGenre(movie.getGenre())
                .setRuntimeMinutes(movie.getRuntimeMinutes())
                .setPosterUrl(movie.getPosterUrl() != null ? movie.getPosterUrl() : "")
                .build())
        );
        responseObserver.onCompleted();
    }
}
