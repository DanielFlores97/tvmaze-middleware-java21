package com.example.pruebatecnica.infrastructure.http;

import com.example.pruebatecnica.application.port.PostProvider;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Post;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JsonPlaceholderPostProvider implements PostProvider {
    private static final Logger log = LoggerFactory.getLogger(JsonPlaceholderPostProvider.class);
    private final RestClient client;

    public JsonPlaceholderPostProvider(RestClient externalRestClient) {
        this.client = externalRestClient;
    }

    @Override
    public Post findById(long id) {
        log.debug("event=upstream.request postId={}", id);
        try {
            ExternalPost response = client.get().uri("/posts/{id}", id).retrieve()
                    .onStatus(status -> status.value() == 404, (request, result) -> {
                        throw new AppException(AppException.Code.POST_NOT_FOUND);
                    })
                    .onStatus(status -> !status.is2xxSuccessful(), (request, result) -> {
                        throw new AppException(AppException.Code.UPSTREAM_UNAVAILABLE);
                    })
                    .body(ExternalPost.class);
            if (response == null || response.id() == null || response.id() != id
                    || response.userId() == null || response.userId() <= 0
                    || response.title() == null || response.body() == null) {
                throw new AppException(AppException.Code.UPSTREAM_UNAVAILABLE);
            }
            return new Post(response.id(), response.userId(), response.title(), response.body());
        } catch (ResourceAccessException exception) {
            throw new AppException(isTimeout(exception)
                    ? AppException.Code.UPSTREAM_TIMEOUT : AppException.Code.UPSTREAM_UNAVAILABLE, exception);
        } catch (RestClientException exception) {
            throw new AppException(AppException.Code.UPSTREAM_UNAVAILABLE, exception);
        }
    }

    private boolean isTimeout(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private record ExternalPost(Long id, Long userId, String title, String body) {
    }
}
