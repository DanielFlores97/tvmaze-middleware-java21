package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.application.SearchShowsService;
import com.example.pruebatecnica.domain.ShowSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
    private final SearchShowsService service;

    public SearchController(SearchShowsService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/search")
    public List<ShowSummary> search(@RequestParam("search_query") @NotBlank @Size(max = 200) String query) {
        return service.search(query);
    }
}
