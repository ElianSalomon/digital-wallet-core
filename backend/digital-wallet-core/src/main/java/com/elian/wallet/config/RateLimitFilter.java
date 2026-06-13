package com.elian.wallet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int maxRequestsPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.requests-per-minute}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr();
        long currentMinute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new Window(currentMinute);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (window.count.get() > maxRequestsPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intenta mas tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class Window {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger(1);

        private Window(long minute) {
            this.minute = minute;
        }
    }
}
