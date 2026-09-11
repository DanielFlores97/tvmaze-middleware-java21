package com.example.pruebatecnica.application.port;

import com.example.pruebatecnica.domain.Show;
import java.util.Optional;

public interface ShowCache {
    Optional<Show> findById(long id);
    void save(Show show);
}
