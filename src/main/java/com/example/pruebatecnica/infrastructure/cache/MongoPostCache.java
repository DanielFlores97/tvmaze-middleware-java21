package com.example.pruebatecnica.infrastructure.cache;

import com.example.pruebatecnica.application.port.PostCache;
import com.example.pruebatecnica.config.CacheProperties;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Post;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class MongoPostCache implements PostCache {
    private static final Logger log = LoggerFactory.getLogger(MongoPostCache.class);
    private final MongoTemplate mongo;
    private final CacheProperties properties;
    private final Clock clock;

    public MongoPostCache(MongoTemplate mongo, CacheProperties properties, Clock clock) {
        this.mongo = mongo;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Optional<Post> findById(long id) {
        try {
            var document = mongo.findById(id, PostCacheDocument.class);
            // TTL cleanup is asynchronous; expired documents must never be served.
            var result = Optional.ofNullable(document)
                    .filter(entry -> entry.expiresAt() != null && entry.expiresAt().isAfter(clock.instant()))
                    .map(PostCacheDocument::post);
            log.debug("event=cache.{} postId={}", result.isPresent() ? "hit" : "miss", id);
            return result;
        } catch (DataAccessException exception) {
            log.warn("event=cache.read_failed postId={} cause={}", id, exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void put(Post post) {
        try {
            mongo.save(new PostCacheDocument(post.id(), post, clock.instant().plus(properties.ttl())));
        } catch (DataAccessException exception) {
            log.warn("event=cache.write_failed postId={} cause={}", post.id(), exception.getClass().getSimpleName());
        }
    }

    @Override
    public void evict(long id) {
        try {
            mongo.remove(Query.query(Criteria.where("_id").is(id)), PostCacheDocument.class);
        } catch (DataAccessException exception) {
            throw new AppException(AppException.Code.CACHE_UNAVAILABLE, exception);
        }
    }
}
