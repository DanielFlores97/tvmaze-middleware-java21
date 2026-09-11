package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.ShowSearchProvider;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.ShowSummary;
import java.util.List;

public class SearchShowsService {
    private final ShowSearchProvider provider;

    public SearchShowsService(ShowSearchProvider provider) {
        this.provider = provider;
    }

    public List<ShowSummary> search(String query) {
        if (query == null || query.isBlank() || query.length() > 200) {
            throw new AppException(AppException.Code.INVALID_REQUEST);
        }
        return provider.search(query.strip());
    }
}
