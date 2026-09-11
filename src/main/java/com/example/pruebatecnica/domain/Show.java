package com.example.pruebatecnica.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record Show(long id, Map<String, Object> attributes) {
    public Show {
        attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
}
