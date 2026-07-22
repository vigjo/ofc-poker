package poker.ofc.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHeadersFilterTest {

    @Test
    void addsBaselineHeadersWithoutHsts() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        filter.hstsEnabled = false;
        MultivaluedMap<String, Object> headers = filterAndCaptureHeaders(filter);

        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(headers.getFirst("Content-Security-Policy")).isEqualTo("default-src 'none'; frame-ancestors 'none'");
        assertThat(headers).doesNotContainKey("Strict-Transport-Security");
    }

    @Test
    void addsHstsWhenEnabled() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        filter.hstsEnabled = true;
        MultivaluedMap<String, Object> headers = filterAndCaptureHeaders(filter);

        assertThat(headers.getFirst("Strict-Transport-Security")).isEqualTo("max-age=63072000; includeSubDomains");
    }

    private MultivaluedMap<String, Object> filterAndCaptureHeaders(SecurityHeadersFilter filter) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(response.getHeaders()).thenReturn(headers);

        filter.filter(request, response);
        return headers;
    }
}
