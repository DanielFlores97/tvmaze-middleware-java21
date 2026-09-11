package com.example.pruebatecnica.infrastructure.persistence;

import com.example.pruebatecnica.application.port.CommentWriter;
import com.example.pruebatecnica.application.port.CommentReader;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Comment;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MongoCommentRepository implements CommentWriter, CommentReader {
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

    @Override
    public Map<Long, List<Comment>> findByShowIds(Collection<Long> showIds) {
        if (showIds.isEmpty()) {
            return Map.of();
        }
        try {
            var query = Query.query(Criteria.where("showId").in(showIds))
                    .with(Sort.by("showId", "createdAt", "_id"));
            Map<Long, List<Comment>> grouped = new LinkedHashMap<>();
            for (var document : mongo.find(query, CommentDocument.class)) {
                grouped.computeIfAbsent(document.showId(), ignored -> new ArrayList<>())
                        .add(new Comment(document.comment(), document.rating()));
            }
            grouped.replaceAll((id, values) -> List.copyOf(values));
            return Map.copyOf(grouped);
        } catch (DataAccessException exception) {
            throw new AppException(AppException.Code.DATABASE_UNAVAILABLE, exception);
        }
    }
}
