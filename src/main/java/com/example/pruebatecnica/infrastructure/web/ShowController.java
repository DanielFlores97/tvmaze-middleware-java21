package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.application.GetShowDetailsService;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShowController {
    private final GetShowDetailsService service;

    public ShowController(GetShowDetailsService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/show")
    public Map<String, Object> show(@RequestParam("show_id") @Positive long id) {
        return service.findById(id);
    }
}
