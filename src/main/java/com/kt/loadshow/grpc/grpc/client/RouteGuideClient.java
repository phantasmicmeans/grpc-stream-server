package com.kt.loadshow.grpc.grpc.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import com.kt.loadshow.grpc.grpc.*;
import com.kt.loadshow.grpc.grpc.RouteGuideGrpc.RouteGuideBlockingStub;
import com.kt.loadshow.grpc.grpc.RouteGuideGrpc.RouteGuideStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RouteGuideClient {

    private final Logger logger= LoggerFactory.getLogger(this.getClass());

    private ManagedChannel channel = null;
    private RouteGuideBlockingStub blockingStub = null;
    private RouteGuideStub asyncStub = null;

    private Random random = new Random();
    private TestHelper testHelper;

    public RouteGuideClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host,port).usePlaintext());
    }

    public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Blocking server-streaming example. Calls listfeatures with a rectangle of interest.
     * Prints each response as it arrives.
     */
    public Iterator<Feature> listFeatures(int lowLat, int lowLon, int hiLat, int hiLon) {
        Rectangle request = Rectangle.newBuilder()
                            .setLo(Point.newBuilder().setLatitude(lowLat).setLatitude(lowLon).build())
                            .setHi(Point.newBuilder().setLatitude(hiLat).setLongtitude(hiLon).build())
                            .build();

        Iterator <Feature> features;
        try {
            features = blockingStub.listFeatures(request); // blocking call to server & response 는 features 로
            for (int i = 1; features.hasNext(); i++) {
                Feature feature = features.next();
                logger.info("Resut #" + i + ": {}", feature);
                if (testHelper != null) {
                    testHelper.onMesssage(feature);
                }
            }
            return features;
        } catch(StatusRuntimeException e) {
            logger.warn("RPC Failed : {}" , e.getStatus());
            if (testHelper != null) {
                testHelper.onRpcError(e);
            }
            return null;
        }
    }

    /**
     * Async client-streaming example. Sends [@code numPoints} randomly choose points from
     * {@code * features} with a varable delay in between. Prints the statistics when they
     * are sent from server
     */
    public void recordRoute(List<Feature> features, int numPoints) throws InterruptedException {
        logger.info("*** RecordRoute");
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver <RouteSummary> responseObserver = new StreamObserver<RouteSummary>() {
            @Override
            public void onNext(RouteSummary routeSummary) {
                logger.info("Distance {} "+  routeSummary.getDistance());
                logger.info("FeatureCount {} " + routeSummary.getFeatureCount());
                logger.info("FeatureCount {} " + routeSummary.getPointCount());
            }

            @Override
            public void onError(Throwable t) {
                Status status = Status.fromThrowable(t);
                logger.warn("RecordRoute Failed {} ",  status);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("finish onCompleted");
                finishLatch.countDown();
            }
        };

        StreamObserver <Point> requestObserver = asyncStub.recordRoute(responseObserver);

        try {//Send numPoints points randomly selected from the selected from the features list.
            for (int i = 0; i < numPoints; ++i) {
                Point point = features.get(i).getLocation();
                logger.info("Visigin point {}, {}", point.getLatitude(), point.getLongtitude());
                requestObserver.onNext(point);
                //Sleep for a bit before sending the next one.
                Thread.sleep( 500);
                if (finishLatch.getCount() == 0) {
                    // RPC completed or errored before we finished sending.
                    // Sending further requests won't error,
                    // but they will just be thrown away.
                    return;
                }
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }

        //mark the end of requests
        requestObserver.onCompleted();

        //Receving happends asynchronously
        finishLatch.await(1, TimeUnit.MINUTES);
    }

    /**
     * Only used for unit test
     */
    @VisibleForTesting
    void setRandom(Random random) {
        this.random = random;
    }

    @VisibleForTesting
    interface TestHelper {
        void onMesssage(Message message);

        void onRpcError(Throwable exception);
    }

    @VisibleForTesting
    void setTestHelper(TestHelper testHelper) {
        this.testHelper = testHelper;
    }
}
