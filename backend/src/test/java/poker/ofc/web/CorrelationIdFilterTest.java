package poker.ofc.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void mintsARequestIdWhenTheCallerDidNotSendOne() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getHeaderString(CorrelationIdFilter.HEADER)).thenReturn(null);

        filter.filter(request);

        verify(request).setProperty(org.mockito.ArgumentMatchers.eq("requestId"),
                org.mockito.ArgumentMatchers.argThat(id -> id instanceof String s && !s.isBlank()));
    }

    @Test
    void reusesTheCallersRequestId() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getHeaderString(CorrelationIdFilter.HEADER)).thenReturn("caller-supplied-id");

        filter.filter(request);

        verify(request).setProperty("requestId", "caller-supplied-id");
    }

    @Test
    void echoesTheRequestIdBackOnTheResponse() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getProperty("requestId")).thenReturn("abc-123");
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(response.getHeaders()).thenReturn(headers);

        filter.filter(request, response);

        assertThat(headers.getFirst(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
    }
}
