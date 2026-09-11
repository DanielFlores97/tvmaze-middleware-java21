package com.example.pruebatecnica.infrastructure.persistence;

import com.example.pruebatecnica.application.port.CommentWriter;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Comment;
import java.time.Clock;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MongoCommentRepository implements CommentWriter {
    private final MongoTemplate mongo;
    private final Clock clock;

    public MongoCommentRepository(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    @Override
    public void save(long showId, Comment comment) {
        try {
            mongo.insert(new CommentDocument(null, showId, comment.comment(), comment.rating(), clock.instant()));
        } catch (DataAccessException exception) {
            throw new AppException(AppException.Code.DATABASE_UNAVAILABLE, exception);
        }
    }
}
