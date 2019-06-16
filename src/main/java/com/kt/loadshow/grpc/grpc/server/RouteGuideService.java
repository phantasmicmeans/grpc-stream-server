package com.kt.loadshow.grpc.grpc.server;

import com.kt.loadshow.grpc.grpc.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

@GrpcService
public class RouteGuideService extends RouteGuideGrpc.RouteGuideImplBase {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void loadImage(SpringRequest request, StreamObserver<CReply> responseObserver) {
        super.loadImage(request, responseObserver);
    }

    @Override
    public void loadImages(Request request, StreamObserver<VAReply> responseObserver) {
        super.loadImages(request, responseObserver);
    }

    /**
     * server-to-client streaming RPC, 서버 -> 클라이언트로 스트리밍
     * @param request
     * @param responseObserver
     */
    @Override
    public void listFeatures(Rectangle request, StreamObserver<Feature> responseObserver) {

        IntStream.range(0,10).forEach(
                idx -> {
                    Feature feature = getFeature(idx, request.getLo());
                    logger.info("idx : {}", idx);
                    responseObserver.onNext(feature);
                }
        );
        responseObserver.onCompleted(); //서버 스트리밍 종료
    }

    /**
     * clinent-to-server Streaming RPC
     * @param responseObserver
     * @return
     */
    @Override
    public StreamObserver<Point> recordRoute(StreamObserver<RouteSummary> responseObserver) {

        return new StreamObserver<Point>() { //client stream control
            int pointcount = 0;

            @Override //클라이언트에서 onNext() 호출때마다 실행
            public void onNext(Point point) {
                logger.info("Point message received {} " , point);
                pointcount++;
            }

            @Override //클라이언트에서 onError()를 호출하거나, 내부 에러가 발생하면 호출
            public void onError(Throwable throwable) {
                logger.error("recordRoute cancelled");
            }

            // 클라이언트에서 모든 메시지 스트림 전송을 마치고, 클라이언트가 자신의 onComplete()를 호출하면 실행
            // 클라이언트는 onComplete를 호출하고, RouteSummary가 리턴될때까지 기다림.
            @Override
            public void onCompleted() {
                RouteSummary summary = RouteSummary.newBuilder()
                        .setPointCount(pointcount)
                        .build();
                responseObserver.onNext(summary); // server -> client response
                responseObserver.onCompleted(); // call completed
            }
        };
    }

    /**
     * Biderectional streaming RPC, 양방향 스트리밍 RPC
     * @param responseObserver
     * @return
     */
    @Override
    public StreamObserver<RouteNote> routeChat(StreamObserver<RouteNote> responseObserver) {

        return new StreamObserver<RouteNote>() {
            // 클라이언트 메시지 전송중에도 서버에서는 클라이언트의 onNext()를 호출하여 메시지 스트림 전송 가능.
            // 서버와 클라이언트의 각각 코드에 작성된 순서대로 메시지를 가져오지만, 순서에 상관없이 읽고 쓴다
            // 즉 각 스트림은 완전히 독립적으로 동작.
            @Override
            public void onNext(RouteNote routeNote) {
                IntStream.range(0,10).forEach(idx -> {
                    responseObserver.onNext(RouteNote.newBuilder(routeNote).build());
                });
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("routeChat Failed");
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted(); //서버도 onCompleted를 호출해 스트리밍 종료를 알림.
            }
        };
    }

    private Feature getFeature(int name ,Point point) {
        return Feature.newBuilder()
                .setName(name + "")
                .setLocation(point)
                .build();
    }
}
