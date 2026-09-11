package com.example.pruebatecnica.application.port;

import com.example.pruebatecnica.domain.Post;
import java.util.Optional;

public interface PostCache {
    Optional<Post> findById(long id);
    void put(Post post);
    void evict(long id);
}
