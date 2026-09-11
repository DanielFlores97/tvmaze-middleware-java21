package com.example.pruebatecnica.config;

import com.example.pruebatecnica.application.PostService;
import com.example.pruebatecnica.application.port.PostCache;
import com.example.pruebatecnica.application.port.PostProvider;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ApplicationConfig {
    @Bean
    PostService postService(PostProvider provider, PostCache cache) {
        return new PostService(provider, cache);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RestClient externalRestClient(RestClient.Builder builder, ExternalApiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        return builder.baseUrl(properties.baseUrl().toString()).requestFactory(factory).build();
    }

    @Bean
    MongoClientSettingsBuilderCustomizer mongoTimeouts() {
        return builder -> builder
                .applyToClusterSettings(settings -> settings.serverSelectionTimeout(3, TimeUnit.SECONDS))
                .applyToSocketSettings(settings -> settings
                        .connectTimeout(3, TimeUnit.SECONDS)
                        .readTimeout(3, TimeUnit.SECONDS));
    }

}
