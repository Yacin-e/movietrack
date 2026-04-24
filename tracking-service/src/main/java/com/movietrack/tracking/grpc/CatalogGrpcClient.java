package com.movietrack.tracking.grpc;

import com.movietrack.grpc.CatalogServiceGrpc;
import com.movietrack.grpc.MovieRequest;
import com.movietrack.grpc.MovieResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CatalogGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcClient.class);

    private final ManagedChannel channel;
    private final CatalogServiceGrpc.CatalogServiceBlockingStub stub;

    public CatalogGrpcClient(
            @Value("${catalog.grpc.host:localhost}") String host,
            @Value("${catalog.grpc.port:9090}") int port) {
        log.info("gRPC client connecting to {}:{}", host, port);
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = CatalogServiceGrpc.newBlockingStub(channel);
    }

    public MovieResponse getMovie(long id) {
        return stub.getMovie(MovieRequest.newBuilder().setId(id).build());
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
    }
}
