package com.example.pruebatecnica.domain;

import java.util.List;

public record ShowSummary(long id, String name, String channel, String summary, List<String> genres) {
    public ShowSummary {
        genres = List.copyOf(genres);
    }
}
