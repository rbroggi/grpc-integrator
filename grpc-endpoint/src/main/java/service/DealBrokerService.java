package service;


import aggregator.Aggregator;
import io.grpc.stub.StreamObserver;
import model.Deal;
import model.DealBrokerGrpc;
import model.ReceiveStatus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Created by rodrigobroggi on 05/05/18.
 */
public class DealBrokerService extends DealBrokerGrpc.DealBrokerImplBase {


    private static final Logger logger = Logger.getLogger(DealBrokerService.class.getName());

    private LinkedBlockingQueue<Deal> incomingEvents = new LinkedBlockingQueue<Deal>();
    Aggregator aggregator = new Aggregator();
    ExecutorService executorService;

    public DealBrokerService() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        aggregator.aggregate(incomingEvents.take());
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE, "Dequeuer broke", e);
                    }
                }

            }
        });
        //Stop service on shutdownHook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down Executor service since JVM is shutting down");
                DealBrokerService.this.shutdown();
                System.err.println("*** executor service shut down");
            }
        });

    }

    void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }



    @Override
    public StreamObserver<Deal> sendDeals(StreamObserver<ReceiveStatus> responseObserver) {
       return new StreamObserver<Deal>() {
           long startTime = System.nanoTime();
           AtomicLong incomingMess = new AtomicLong(0l);
           @Override
           public void onNext(Deal deal) {
               boolean inserted = incomingEvents.offer(deal);
               logger.log(Level.FINEST, inserted ? () -> "Message enqueued": () -> "Message couldn't be enqueued");
               incomingMess.incrementAndGet();
           }

           @Override

           public void onError(Throwable throwable) {
               logger.log(Level.WARNING, "Error on message: ", throwable);
           }

           @Override
           public void onCompleted() {
               long millis = NANOSECONDS.toMillis(System.nanoTime() - startTime);
               responseObserver.onNext(ReceiveStatus.newBuilder().setNumDeals(incomingMess.longValue()).setStatusOk(true).build());
               responseObserver.onCompleted();
               logger.info("Elapsed time: " + millis + " millis");

           }
       };
    }

    @Override
    public void sendDeal(Deal request, StreamObserver<ReceiveStatus> responseObserver) {
        responseObserver.onNext(enqueueSingleTrade(request));
        responseObserver.onCompleted();
    }


    private ReceiveStatus enqueueSingleTrade(Deal deal) {
        boolean inserted = incomingEvents.offer(deal);
        logger.log(Level.FINEST, inserted ? () -> "Message enqueued": () -> "Message couldn't be enqueued");
        return ReceiveStatus.newBuilder().setNumDeals(1).setStatusOk(true).build();
    }

}
