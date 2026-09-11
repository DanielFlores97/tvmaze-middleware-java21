package com.example.pruebatecnica.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        String suppliedId = request.getHeader("X-Request-ID");
        String requestId = suppliedId != null && suppliedId.matches("[a-zA-Z0-9._-]{1,64}")
                ? suppliedId : UUID.randomUUID().toString();
        long start = System.nanoTime();
        try (var ignored = MDC.putCloseable("requestId", requestId)) {
            response.setHeader("X-Request-ID", requestId);
            try {
                chain.doFilter(request, response);
            } finally {
                log.info("event=http.request method={} status={} durationMs={}",
                        request.getMethod(), response.getStatus(),
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            }
        }
    }
}
