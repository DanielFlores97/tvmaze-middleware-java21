package com.example.pruebatecnica.domain;

import java.util.List;

public record ShowSearchResult(long id, String name, String channel, String summary,
                               List<String> genres, List<Comment> comments) {
    public ShowSearchResult {
        genres = List.copyOf(genres);
        comments = List.copyOf(comments);
    }

    public static ShowSearchResult from(ShowSummary show, List<Comment> comments) {
        return new ShowSearchResult(show.id(), show.name(), show.channel(), show.summary(), show.genres(), comments);
    }
}
