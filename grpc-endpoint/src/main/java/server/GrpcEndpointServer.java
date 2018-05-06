package server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import service.DealBrokerService;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by rodrigobroggi on 05/05/18.
 */
public class GrpcEndpointServer {


        private static final Logger logger = Logger.getLogger(GrpcEndpointServer.class.getName());

        static public int port_test = 50051;
        private Server server;
        private int port;

        public GrpcEndpointServer(int port) {
            this.port = port;

        }
        private void start() throws IOException {
    /* The port on which the server should run */
            server = ServerBuilder.forPort(port)
                    .addService(new DealBrokerService())
                    .build()
                    .start();
            logger.info("GrpcEndpointServer started, listening on " + port);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                    System.err.println("*** shutting down gRPC server since JVM is shutting down");
                    GrpcEndpointServer.this.stop();
                    System.err.println("*** server shut down");
                }
            });
        }

        private void stop() {
            if (server != null) {
                server.shutdown();
            }
        }

        /**
         * Await termination on the main thread since the grpc library uses daemon threads.
         */
        private void blockUntilShutdown() throws InterruptedException {
            if (server != null) {
                server.awaitTermination();
            }
        }


    public static void main(String[] args) throws IOException {
        GrpcEndpointServer server = new GrpcEndpointServer(port_test);
        server.start();

    }
}
