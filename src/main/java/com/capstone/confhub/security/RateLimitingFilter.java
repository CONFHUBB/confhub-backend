package com.capstone.confhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Queue<Long>> requestCounts = new ConcurrentHashMap<>();

    // Limits
    private static final int AUTH_LIMIT = 5;      // 5 requests
    private static final long AUTH_WINDOW = 60_000; // per minute

    private static final int DOC_LIMIT = 20;      // 20 requests
    private static final long DOC_WINDOW = 60_000;  // per minute

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Check if path is rate-limited
        boolean isAuth = path.startsWith("/api/v1/auth/signin");
        boolean isDoc = path.startsWith("/api/v1/documents");

        if (isAuth || isDoc) {
            String clientIp = getClientIP(request);
            String key = (isAuth ? "auth:" : "doc:") + clientIp;
            int limit = isAuth ? AUTH_LIMIT : DOC_LIMIT;
            long window = isAuth ? AUTH_WINDOW : DOC_WINDOW;

            if (!isAllowed(key, limit, window)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String key, int limit, long windowInMillis) {
        long now = System.currentTimeMillis();
        Queue<Long> requests = requestCounts.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // Remove old requests
        while (!requests.isEmpty() && now - requests.peek() > windowInMillis) {
            requests.poll();
        }

        if (requests.size() < limit) {
            requests.offer(now);
            return true;
        }

        return false;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
