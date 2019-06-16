package com.kt.loadshow.grpc.grpc.api;

import com.kt.loadshow.grpc.grpc.Feature;
import com.kt.loadshow.grpc.grpc.Point;
import com.kt.loadshow.grpc.grpc.client.RouteGuideClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

@RestController
public class gRpcController {

    @Autowired
    private RouteGuideClient routeGuideClient;

    @GetMapping("/route")
    public List<?> serverStreamApi() {
        Iterator<Feature> features = routeGuideClient.listFeatures(1,2,3,4);
        List<String> list = new ArrayList<>();
        features.forEachRemaining(feature -> list.add(feature.getName()));

        return !list.isEmpty() ? list : new ArrayList<>();
    }

    @GetMapping("/routeChat")
    public void clientStreamApi() throws InterruptedException {
        List<Feature> list = new ArrayList<>();
        IntStream.range(0, 10).forEach(
                idx -> list.add(Feature.newBuilder()
                                    .setName(idx + "")
                                    .setLocation(Point.newBuilder().setLatitude(idx).setLongtitude(idx + 1))
                                    .build()));

        routeGuideClient.recordRoute(list, 10);
    }
}
