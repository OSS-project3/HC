package com.example.honorcitizen.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * OAuth 완료 후 돌아갈 프론트 origin을 현재 요청 Host 기준으로 결정한다.
 *
 * <p>요청 Host를 그대로 신뢰하면 Host header를 이용한 open redirect가 가능하므로,
 * 운영 환경에서 명시한 origin allowlist와 정확히 일치할 때만 사용한다.</p>
 */
@Component
public class FrontendOriginResolver {

    private final String fallbackOrigin;
    private final Set<String> allowedOrigins;

    public FrontendOriginResolver(
            @Value("${app.frontend-url}") String fallbackOrigin,
            @Value("${app.frontend-allowed-origins:${app.frontend-url}}") String allowedOrigins) {
        this.fallbackOrigin = normalizeConfiguredOrigin(fallbackOrigin);
        this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
        if (!this.allowedOrigins.contains(this.fallbackOrigin)) {
            throw new IllegalArgumentException("app.frontend-url must be included in app.frontend-allowed-origins");
        }
    }

    public String resolve(HttpServletRequest request) {
        String requestOrigin = normalizeOrigin(request.getScheme(), request.getServerName(), request.getServerPort());
        return allowedOrigins.contains(requestOrigin) ? requestOrigin : fallbackOrigin;
    }

    private Set<String> parseAllowedOrigins(String configuredOrigins) {
        Set<String> origins = new LinkedHashSet<>();
        Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(this::normalizeConfiguredOrigin)
                .forEach(origins::add);
        if (origins.isEmpty()) {
            throw new IllegalArgumentException("app.frontend-allowed-origins must not be empty");
        }
        return Set.copyOf(origins);
    }

    private String normalizeConfiguredOrigin(String value) {
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid frontend origin: " + value, e);
        }

        if (uri.getScheme() == null || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath()))) {
            throw new IllegalArgumentException("Frontend origin must contain only scheme, host and optional port: " + value);
        }
        return normalizeOrigin(uri.getScheme(), uri.getHost(), uri.getPort());
    }

    private String normalizeOrigin(String scheme, String host, int port) {
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
            throw new IllegalArgumentException("Unsupported frontend origin scheme: " + scheme);
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        boolean defaultPort = port < 0
                || ("http".equals(normalizedScheme) && port == 80)
                || ("https".equals(normalizedScheme) && port == 443);
        return normalizedScheme + "://" + normalizedHost + (defaultPort ? "" : ":" + port);
    }
}
