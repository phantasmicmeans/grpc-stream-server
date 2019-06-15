package com.kt.loadshow.grpc.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class RouteGuideServer {

    private final int port;
    private final Server server;

    public RouteGuideServer(int port) throws IOException {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(new RouteGuideService())
                .build();
    }

    public void start() throws IOException {
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                RouteGuideServer.this.stop();
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /*
    public static void main(String args[]) throws Exception{
        RouteGuideServer server = new RouteGuideServer(8090);
        server.start();;
        server.blockUntilShutdown();
    }*/
}
