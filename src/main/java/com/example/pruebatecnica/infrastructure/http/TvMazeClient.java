package com.example.pruebatecnica.infrastructure.http;

import com.example.pruebatecnica.application.port.ShowSearchProvider;
import com.example.pruebatecnica.application.port.ShowProvider;
import com.example.pruebatecnica.domain.Show;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.ShowSummary;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TvMazeClient implements ShowSearchProvider, ShowProvider {
    private final RestClient client;
    private final ObjectMapper mapper;

    public TvMazeClient(RestClient externalRestClient, ObjectMapper mapper) {
        this.client = externalRestClient;
        this.mapper = mapper;
    }

    @Override
    public Show findById(long id) {
        JsonNode response = get("/shows/{id}", id);
        if (validId(response) != id || !response.path("name").isTextual()) {
            throw invalidResponse();
        }
        Map<String, Object> attributes = mapper.convertValue(response, new TypeReference<>() {});
        return new Show(id, attributes);
    }

    @Override
    public List<ShowSummary> search(String query) {
        JsonNode response = get("/search/shows?q={query}", query);
        if (!response.isArray()) {
            throw invalidResponse();
        }
        List<ShowSummary> results = new ArrayList<>();
        for (JsonNode item : response) {
            JsonNode show = item.path("show");
            long id = validId(show);
            String name = textOrNull(show.path("name"));
            if (name == null || name.isBlank() || !show.path("genres").isArray()) {
                throw invalidResponse();
            }
            List<String> genres = new ArrayList<>();
            for (JsonNode genre : show.path("genres")) {
                if (!genre.isTextual()) {
                    throw invalidResponse();
                }
                genres.add(genre.textValue());
            }
            String network = textOrNull(show.path("network").path("name"));
            String webChannel = textOrNull(show.path("webChannel").path("name"));
            String channel = network == null || network.isBlank() ? webChannel : network;
            results.add(new ShowSummary(id, name, channel, textOrNull(show.path("summary")), genres));
        }
        return List.copyOf(results);
    }

    private JsonNode get(String path, Object parameter) {
        try {
            JsonNode response = client.get().uri(path, parameter).retrieve()
                    .onStatus(status -> status.value() == 404, (request, result) -> {
                        throw new AppException(AppException.Code.SHOW_NOT_FOUND);
                    })
                    .onStatus(status -> status.value() == 429, (request, result) -> {
                        throw new AppException(AppException.Code.UPSTREAM_RATE_LIMITED);
                    })
                    .onStatus(status -> !status.is2xxSuccessful(), (request, result) -> {
                        throw invalidResponse();
                    })
                    .body(JsonNode.class);
            if (response == null || response.isNull()) {
                throw invalidResponse();
            }
            return response;
        } catch (ResourceAccessException exception) {
            throw new AppException(isTimeout(exception)
                    ? AppException.Code.UPSTREAM_TIMEOUT : AppException.Code.UPSTREAM_UNAVAILABLE, exception);
        } catch (RestClientException exception) {
            throw new AppException(AppException.Code.UPSTREAM_UNAVAILABLE, exception);
        }
    }

    private long validId(JsonNode show) {
        JsonNode id = show.path("id");
        if (!show.isObject() || !id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() <= 0) {
            throw invalidResponse();
        }
        return id.longValue();
    }

    private String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw invalidResponse();
        }
        return node.textValue();
    }

    private AppException invalidResponse() {
        return new AppException(AppException.Code.UPSTREAM_UNAVAILABLE);
    }

    private boolean isTimeout(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
