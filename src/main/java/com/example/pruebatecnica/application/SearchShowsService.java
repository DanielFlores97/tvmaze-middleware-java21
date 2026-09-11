package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.ShowSearchProvider;
import com.example.pruebatecnica.application.port.CommentReader;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.ShowSummary;
import com.example.pruebatecnica.domain.ShowSearchResult;
import java.util.List;

public class SearchShowsService {
    private final ShowSearchProvider provider;
    private final CommentReader comments;

    public SearchShowsService(ShowSearchProvider provider, CommentReader comments) {
        this.provider = provider;
        this.comments = comments;
    }

    public List<ShowSearchResult> search(String query) {
        if (query == null || query.isBlank() || query.length() > 200) {
            throw new AppException(AppException.Code.INVALID_REQUEST);
        }
        var shows = provider.search(query.strip());
        if (shows.isEmpty()) {
            return List.of();
        }
        var ids = shows.stream().map(ShowSummary::id).distinct().toList();
        var byShow = comments.findByShowIds(ids);
        return shows.stream()
                .map(show -> ShowSearchResult.from(show, byShow.getOrDefault(show.id(), List.of())))
                .toList();
    }
}
