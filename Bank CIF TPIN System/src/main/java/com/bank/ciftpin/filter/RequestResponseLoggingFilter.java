package com.bank.ciftpin.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class RequestResponseLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Skip logging for H2 console, swagger, and actuator endpoints
        String uri = request.getRequestURI();
        if (uri.contains("/h2-console") || uri.contains("/swagger")
                || uri.contains("/api-docs") || uri.contains("/webjars")) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Set a reasonable limit in bytes (e.g., 10240 for 10KB, or 1048576 for 1MB)
        int cacheLimit = 10240;
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, cacheLimit);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        String requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log request
            String requestBody = getBodyAsString(requestWrapper.getContentAsByteArray());
            String maskedRequestBody = maskSensitiveData(requestBody);

            log.info("======================================================");
            log.info("[REQUEST-{}] {} {} | IP: {} | Body: {}",
                    requestId,
                    request.getMethod(),
                    uri,
                    request.getRemoteAddr(),
                    maskedRequestBody.isEmpty() ? "(empty)" : maskedRequestBody);

            // Log response
            String responseBody = getBodyAsString(responseWrapper.getContentAsByteArray());
            log.info("[RESPONSE-{}] Status: {} | Duration: {}ms | Body: {}",
                    requestId,
                    response.getStatus(),
                    duration,
                    responseBody.isEmpty() ? "(empty)" : responseBody);
            log.info("======================================================");

            // IMPORTANT: copy response body back to the actual response
            responseWrapper.copyBodyToResponse();
        }
    }

    private String getBodyAsString(byte[] contentAsByteArray) {
        if (contentAsByteArray == null || contentAsByteArray.length == 0) {
            return "";
        }
        return new String(contentAsByteArray, StandardCharsets.UTF_8);
    }

    /**
     * Masks sensitive fields like tpin, newTpin, tpinHash in log output.
     */
    private String maskSensitiveData(String body) {
        if (body == null || body.isEmpty()) return body;
        // Mask tpin values: "tpin":"1234" -> "tpin":"****"
        return body
                .replaceAll("(?i)(\"tpin\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3")
                .replaceAll("(?i)(\"newTpin\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");
    }
}