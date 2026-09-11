package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.CommentWriter;
import com.example.pruebatecnica.domain.Comment;
import java.math.BigDecimal;

public class AddCommentService {
    private final GetShowService shows;
    private final CommentWriter comments;

    public AddCommentService(GetShowService shows, CommentWriter comments) {
        this.shows = shows;
        this.comments = comments;
    }

    public void add(long showId, String text, BigDecimal rating) {
        Comment comment = new Comment(text, rating);
        shows.findById(showId);
        comments.save(showId, comment);
    }
}
