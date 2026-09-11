package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.ShowProvider;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Show;

public class GetShowService {
    private final ShowProvider provider;

    public GetShowService(ShowProvider provider) {
        this.provider = provider;
    }

    public Show findById(long id) {
        if (id <= 0) {
            throw new AppException(AppException.Code.INVALID_REQUEST);
        }
        return provider.findById(id);
    }
}
