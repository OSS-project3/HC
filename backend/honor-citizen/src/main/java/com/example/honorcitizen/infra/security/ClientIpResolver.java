package com.example.honorcitizen.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

// rate limit 등에 쓰는 클라이언트 IP를 구한다. 설정된 신뢰 프록시(app.security.trusted-proxies)에서
// 온 요청만 X-Forwarded-For를 인정하고, 그 외(직접 요청·비신뢰 프록시)는 remoteAddr을 그대로 쓴다.
// 값을 위조한 X-Forwarded-For로 rate limit을 우회하는 것을 막기 위함이다.
@Component
public class ClientIpResolver {

    private static final Pattern CIDR_PATTERN = Pattern.compile("^(\\d{1,3}(?:\\.\\d{1,3}){3})/(\\d{1,2})$");

    private final List<String> trustedProxies;

    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String trustedProxiesRaw) {
        this.trustedProxies = StringUtils.hasText(trustedProxiesRaw)
                ? Arrays.stream(trustedProxiesRaw.split(",")).map(String::trim).filter(StringUtils::hasText).toList()
                : List.of();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.isEmpty() || !isTrusted(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(forwardedFor)) {
            return remoteAddr;
        }
        // 왼쪽 첫 값이 원 클라이언트 IP(체인의 나머지는 중간 프록시들).
        String firstHop = forwardedFor.split(",")[0].trim();
        return StringUtils.hasText(firstHop) ? firstHop : remoteAddr;
    }

    private boolean isTrusted(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }
        for (String entry : trustedProxies) {
            if (entry.contains("/") ? matchesCidr(remoteAddr, entry) : entry.equals(remoteAddr)) {
                return true;
            }
        }
        return false;
    }

    // IPv4 CIDR만 지원한다(현재 배포 환경이 IPv4 전용).
    private boolean matchesCidr(String ip, String cidr) {
        var matcher = CIDR_PATTERN.matcher(cidr);
        if (!matcher.matches()) {
            return false;
        }
        try {
            int prefixLength = Integer.parseInt(matcher.group(2));
            if (prefixLength < 0 || prefixLength > 32) {
                return false;
            }
            long ipValue = ipv4ToLong(ip);
            long networkValue = ipv4ToLong(matcher.group(1));
            long mask = prefixLength == 0 ? 0L : (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            return (ipValue & mask) == (networkValue & mask);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("IPv4 형식이 아닙니다: " + ip);
        }
        long result = 0;
        for (String part : parts) {
            int octet = Integer.parseInt(part);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("IPv4 옥텟 범위 초과: " + ip);
            }
            result = (result << 8) | octet;
        }
        return result;
    }
}
