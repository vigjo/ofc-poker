package poker.ofc.web;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        filter.windowSeconds = 60;
        filter.maxRequestsPerWindow = 3;
        filter.vertxRequest = requestFrom("203.0.113.5");
    }

    @Test
    void allowsRequestsUnderTheLimit() {
        ContainerRequestContext context = requestContext("POST", "api/rooms");

        filter.filter(context);
        filter.filter(context);
        filter.filter(context);

        verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blocksRequestsOverTheLimitWith429() {
        ContainerRequestContext context = requestContext("POST", "api/rooms");

        filter.filter(context);
        filter.filter(context);
        filter.filter(context);
        filter.filter(context);

        var captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        verify(context).abortWith(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(429);
    }

    @Test
    void tracksDifferentClientsIndependently() {
        ContainerRequestContext context = requestContext("POST", "api/rooms");

        filter.filter(context);
        filter.filter(context);
        filter.filter(context);
        filter.filter(context);
        verify(context).abortWith(org.mockito.ArgumentMatchers.any());

        filter.vertxRequest = requestFrom("198.51.100.9");
        ContainerRequestContext other = requestContext("POST", "api/rooms");
        filter.filter(other);
        verify(other, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotRateLimitUnrelatedEndpoints() {
        ContainerRequestContext context = requestContext("POST", "api/rooms/ABC123/moves");

        for (int i = 0; i < 10; i++) {
            filter.filter(context);
        }

        verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }

    private ContainerRequestContext requestContext(String method, String path) {
        ContainerRequestContext context = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(path);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getMethod()).thenReturn(method);
        return context;
    }

    private HttpServerRequest requestFrom(String host) {
        HttpServerRequest request = mock(HttpServerRequest.class);
        SocketAddress address = mock(SocketAddress.class);
        when(address.host()).thenReturn(host);
        when(request.remoteAddress()).thenReturn(address);
        return request;
    }
}
