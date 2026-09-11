package com.example.pruebatecnica.application.port;

import com.example.pruebatecnica.domain.ShowSummary;
import java.util.List;

public interface ShowSearchProvider {
    List<ShowSummary> search(String query);
}
