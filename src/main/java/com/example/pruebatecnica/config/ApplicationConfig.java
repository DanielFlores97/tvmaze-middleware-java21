package com.example.pruebatecnica.config;

import com.example.pruebatecnica.application.SearchShowsService;
import com.example.pruebatecnica.application.GetShowService;
import com.example.pruebatecnica.application.port.ShowProvider;
import com.example.pruebatecnica.application.port.ShowCache;
import com.example.pruebatecnica.application.port.ShowSearchProvider;
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
    GetShowService getShowService(ShowProvider provider, ShowCache cache) {
        return new GetShowService(provider, cache);
    }

    @Bean
    SearchShowsService searchShowsService(ShowSearchProvider provider) {
        return new SearchShowsService(provider);
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
        return builder.baseUrl(properties.baseUrl().toString())
                .defaultHeader("User-Agent", "Pinwox-TVmaze-Technical-Test/1.0")
                .requestFactory(factory).build();
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
