package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.application.PostService;
import com.example.pruebatecnica.domain.Post;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {
    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public Post findById(@PathVariable @Positive long id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}/cache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evict(@PathVariable @Positive long id) {
        service.evict(id);
    }
}
