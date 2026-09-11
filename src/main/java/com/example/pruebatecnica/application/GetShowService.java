package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.ShowProvider;
import com.example.pruebatecnica.application.port.ShowCache;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Show;

public class GetShowService {
    private final ShowProvider provider;
    private final ShowCache cache;

    public GetShowService(ShowProvider provider, ShowCache cache) {
        this.provider = provider;
        this.cache = cache;
    }

    public Show findById(long id) {
        if (id <= 0) {
            throw new AppException(AppException.Code.INVALID_REQUEST);
        }
        return cache.findById(id).orElseGet(() -> {
            Show show = provider.findById(id);
            cache.save(show);
            return show;
        });
    }
}
