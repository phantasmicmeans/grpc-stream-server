package com.kt.loadshow.grpc.grpc;

import com.kt.loadshow.grpc.grpc.client.RouteGuideClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfiguration {

    @Bean
    public RouteGuideClient routeGuideClient() {
        return new RouteGuideClient("127.0.0.1", 9090);
    }
}
