package com.movietrack.catalog.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrpcServerRunner {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);

    private final CatalogGrpcService catalogGrpcService;
    private Server server;

    public GrpcServerRunner(CatalogGrpcService catalogGrpcService) {
        this.catalogGrpcService = catalogGrpcService;
    }

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder.forPort(9090)
                .addService(catalogGrpcService)
                .build()
                .start();
        log.info("gRPC server started on port 9090");
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
            log.info("gRPC server stopped");
        }
    }
}
