package com.example.pruebatecnica.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("show_comments")
@CompoundIndex(name = "comments_by_show", def = "{'showId': 1, 'createdAt': 1, '_id': 1}")
public record CommentDocument(
        @Id String id,
        long showId,
        String comment,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal rating,
        Instant createdAt) {
}
