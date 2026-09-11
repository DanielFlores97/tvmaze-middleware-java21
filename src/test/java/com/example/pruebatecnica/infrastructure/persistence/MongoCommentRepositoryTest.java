package com.example.pruebatecnica.infrastructure.persistence;

import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Comment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MongoCommentRepositoryTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final Instant now = Instant.parse("2026-09-11T12:00:00Z");
    private final MongoCommentRepository repository = new MongoCommentRepository(mongo, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void insertsIndependentCommentLinkedToShow() {
        repository.save(42, new Comment("Good", new BigDecimal("4.5")));
        var captor = ArgumentCaptor.forClass(CommentDocument.class);
        verify(mongo).insert(captor.capture());
        assertThat(captor.getValue().showId()).isEqualTo(42);
        assertThat(captor.getValue().rating()).isEqualByComparingTo("4.5");
        assertThat(captor.getValue().createdAt()).isEqualTo(now);
        assertThat(captor.getValue().id()).isNull();
    }

    @Test
    void insertFailureIsPropagated() {
        when(mongo.insert(any(CommentDocument.class))).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatThrownBy(() -> repository.save(1, new Comment("Good", BigDecimal.ONE)))
                .isInstanceOf(AppException.class);
    }
}
