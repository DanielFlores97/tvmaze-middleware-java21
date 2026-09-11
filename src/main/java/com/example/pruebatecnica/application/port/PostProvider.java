package com.example.pruebatecnica.application.port;

import com.example.pruebatecnica.domain.Post;

public interface PostProvider {
    Post findById(long id);
}
