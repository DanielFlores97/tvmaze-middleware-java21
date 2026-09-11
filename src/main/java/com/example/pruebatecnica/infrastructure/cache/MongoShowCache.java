package com.example.pruebatecnica.infrastructure.cache;

import com.example.pruebatecnica.application.port.ShowCache;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Show;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoShowCache implements ShowCache {
    private static final Logger log = LoggerFactory.getLogger(MongoShowCache.class);
    private final MongoTemplate mongo;
    private final ObjectMapper mapper;
    private final Clock clock;

    public MongoShowCache(MongoTemplate mongo, ObjectMapper mapper, Clock clock) {
        this.mongo = mongo;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public Optional<Show> findById(long id) {
        try {
            var document = mongo.findById(id, ShowCacheDocument.class);
            if (document == null) {
                log.debug("event=cache.miss showId={}", id);
                return Optional.empty();
            }
            Map<String, Object> attributes = mapper.readValue(document.payload(), new TypeReference<>() {});
            log.debug("event=cache.hit showId={}", id);
            return Optional.of(new Show(id, attributes));
        } catch (DataAccessException | JsonProcessingException exception) {
            throw new AppException(AppException.Code.DATABASE_UNAVAILABLE, exception);
        }
    }

    @Override
    public void save(Show show) {
        try {
            // Store the original JSON as a string to preserve arbitrary nested TVMaze fields.
            String payload = mapper.writeValueAsString(show.attributes());
            mongo.save(new ShowCacheDocument(show.id(), payload, clock.instant()));
        } catch (DataAccessException | JsonProcessingException exception) {
            throw new AppException(AppException.Code.DATABASE_UNAVAILABLE, exception);
        }
    }
}
