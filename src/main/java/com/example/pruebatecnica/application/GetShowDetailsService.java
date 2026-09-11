package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.CommentReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetShowDetailsService {
    private final GetShowService shows;
    private final CommentReader comments;

    public GetShowDetailsService(GetShowService shows, CommentReader comments) {
        this.shows = shows;
        this.comments = comments;
    }

    public Map<String, Object> findById(long id) {
        var show = shows.findById(id);
        var result = new LinkedHashMap<>(show.attributes());
        // Comments are read on every request, never embedded in the cached TVMaze payload.
        result.put("comments", comments.findByShowIds(List.of(id)).getOrDefault(id, List.of()));
        return result;
    }
}
