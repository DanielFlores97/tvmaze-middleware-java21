package com.example.pruebatecnica.application.port;

import com.example.pruebatecnica.domain.Show;

public interface ShowProvider {
    Show findById(long id);
}
