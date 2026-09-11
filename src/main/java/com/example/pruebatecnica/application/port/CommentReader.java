package com.example.pruebatecnica.application.port;

import com.example.pruebatecnica.domain.Comment;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CommentReader {
    Map<Long, List<Comment>> findByShowIds(Collection<Long> showIds);
}
