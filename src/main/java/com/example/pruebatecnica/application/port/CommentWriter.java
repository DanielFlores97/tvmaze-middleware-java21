package com.example.pruebatecnica.application.port;

import com.example.pruebatecnica.domain.Comment;

public interface CommentWriter {
    void save(long showId, Comment comment);
}
