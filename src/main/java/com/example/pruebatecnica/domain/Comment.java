package com.example.pruebatecnica.domain;

import java.math.BigDecimal;

public record Comment(String comment, BigDecimal rating) {
    public Comment {
        if (comment == null || comment.isBlank() || comment.length() > 2000 || rating == null
                || rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new AppException(AppException.Code.INVALID_REQUEST);
        }
        comment = comment.strip();
    }
}
