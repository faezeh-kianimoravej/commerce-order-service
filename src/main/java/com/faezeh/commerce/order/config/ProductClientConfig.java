package com.faezeh.commerce.order.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ProductClientConfig {

    @Bean
    public RestClient productRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${product.service.url}") String productServiceUrl
    ) {
        return restClientBuilder
                .baseUrl(productServiceUrl)
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService productServiceExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
