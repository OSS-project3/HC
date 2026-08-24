package com.example.honorcitizen.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrontendOriginResolverTest {

    private final FrontendOriginResolver resolver = new FrontendOriginResolver(
            "https://name.hanse.kr",
            "https://name.hanse.kr,https://hanse.kr");

    @Test
    void resolvesCorporationDomainFromRequestHost() {
        MockHttpServletRequest request = request("https", "name.hanse.kr", 443);

        assertThat(resolver.resolve(request)).isEqualTo("https://name.hanse.kr");
    }

    @Test
    void resolvesAssociationDomainFromRequestHost() {
        MockHttpServletRequest request = request("https", "hanse.kr", 443);

        assertThat(resolver.resolve(request)).isEqualTo("https://hanse.kr");
    }

    @Test
    void fallsBackForHostOutsideAllowlist() {
        MockHttpServletRequest request = request("https", "attacker.example", 443);

        assertThat(resolver.resolve(request)).isEqualTo("https://name.hanse.kr");
    }

    @Test
    void preservesNonDefaultDevelopmentPort() {
        FrontendOriginResolver localResolver = new FrontendOriginResolver(
                "http://localhost:3000",
                "http://localhost:3000");
        MockHttpServletRequest request = request("http", "localhost", 3000);

        assertThat(localResolver.resolve(request)).isEqualTo("http://localhost:3000");
    }

    @Test
    void rejectsFallbackMissingFromAllowlist() {
        assertThatThrownBy(() -> new FrontendOriginResolver(
                "https://name.hanse.kr",
                "https://hanse.kr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be included");
    }

    @Test
    void rejectsOriginContainingPath() {
        assertThatThrownBy(() -> new FrontendOriginResolver(
                "https://name.hanse.kr/login",
                "https://name.hanse.kr/login"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheme, host and optional port");
    }

    private MockHttpServletRequest request(String scheme, String host, int port) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName(host);
        request.setServerPort(port);
        return request;
    }
}
