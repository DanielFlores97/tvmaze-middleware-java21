package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.PostCache;
import com.example.pruebatecnica.application.port.PostProvider;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Post;

public class PostService {
    private final PostProvider provider;
    private final PostCache cache;

    public PostService(PostProvider provider, PostCache cache) {
        this.provider = provider;
        this.cache = cache;
    }

    public Post findById(long id) {
        validateId(id);
        return cache.findById(id).orElseGet(() -> {
            Post post = provider.findById(id);
            cache.put(post);
            return post;
        });
    }

    public void evict(long id) {
        validateId(id);
        cache.evict(id);
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new AppException(AppException.Code.INVALID_REQUEST);
        }
    }
}
