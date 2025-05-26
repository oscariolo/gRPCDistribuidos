package grpc_trabajo.bootstrap;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import grpc_trabajo.config.AppConfig;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

public class ServerBootstrap {
    private static final Logger logger = Logger.getLogger(ServerBootstrap.class.getName());
    private Server server;
    private ExecutorService executor;

    public void start(AppConfig config) throws IOException, InterruptedException {
        executor = java.util.concurrent.Executors.newFixedThreadPool(2); //hilos que utilizara para manejar las peticiones rpc
        server = Grpc.newServerBuilderForPort(config.getPort(), InsecureServerCredentials.create()) //inicializar el servidor grpc
        .executor(executor)
        .addServices(config.getServices()) //agregamos los servicios grpc al servidor definidos en config
        .build()
        .start(); //inicia el servidor con los servicios agregados
        logger.info("Server started, listening on " + config.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
            ServerBootstrap.this.stop();
            } catch (InterruptedException e) {
            if (server != null) {
                server.shutdownNow();
            }
            e.printStackTrace(System.err);
            } finally {
            executor.shutdown();
            }
            System.err.println("*** server shut down");
        }
        });
        blockUntilShutdown();  
    }

    private void stop() throws InterruptedException {
        if (server != null) {
        server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }


    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
        server.awaitTermination();
        }
    }
    
}
