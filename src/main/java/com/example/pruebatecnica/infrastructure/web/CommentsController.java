package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.application.AddCommentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentsController {
    private final AddCommentService service;

    public CommentsController(AddCommentService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentStatus create(@Valid @RequestBody CommentRequest request) {
        service.add(request.showId(), request.comment(), request.rating());
        return new CommentStatus("created");
    }

    public record CommentRequest(
            @JsonProperty("show_id") @NotNull @Positive Long showId,
            @NotBlank @Size(max = 2000) String comment,
            @NotNull @DecimalMin("0") @DecimalMax("5") BigDecimal rating) {
    }

    public record CommentStatus(String status) {
    }
}
