package com.example.honorcitizen.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {

    @Test
    void usesRemoteAddrWhenNoTrustedProxiesConfigured() {
        ClientIpResolver resolver = new ClientIpResolver("");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void usesRemoteAddrWhenRequestComesFromUntrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver("172.18.0.5");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void usesFirstForwardedForHopWhenRequestComesFromTrustedProxyIp() {
        ClientIpResolver resolver = new ClientIpResolver("172.18.0.5");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("172.18.0.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1, 172.18.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.1");
    }

    @Test
    void usesFirstForwardedForHopWhenRequestComesFromTrustedProxyCidr() {
        ClientIpResolver resolver = new ClientIpResolver("172.18.0.0/16");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("172.18.5.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.1");
    }

    @Test
    void ignoresCidrOutsideTrustedRange() {
        ClientIpResolver resolver = new ClientIpResolver("172.18.0.0/16");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("172.19.5.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");

        assertThat(resolver.resolve(request)).isEqualTo("172.19.5.9");
    }

    @Test
    void fallsBackToRemoteAddrWhenTrustedProxyButHeaderMissing() {
        ClientIpResolver resolver = new ClientIpResolver("172.18.0.5");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("172.18.0.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        assertThat(resolver.resolve(request)).isEqualTo("172.18.0.5");
    }

    @Test
    void supportsMultipleTrustedProxyEntries() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.1, 172.18.0.0/16");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.1");
    }
}
