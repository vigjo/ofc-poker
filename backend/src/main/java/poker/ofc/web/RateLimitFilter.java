package poker.ofc.web;

import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small fixed-window rate limiter guarding the only two endpoints an
 * anonymous caller can hit without a seat token: creating and joining rooms.
 * It is intentionally simple (per-process, in-memory) rather than a
 * distributed limiter, which matches the single-task launch runtime
 * described in docs/adr/0004-single-task-launch-runtime.md; revisit if a
 * second task is added.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 10)
public class RateLimitFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(RateLimitFilter.class);

    private record Window(long windowStartMillis, AtomicInteger count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @ConfigProperty(name = "ofc.rate-limit.window-seconds", defaultValue = "60")
    long windowSeconds;

    @ConfigProperty(name = "ofc.rate-limit.max-requests-per-window", defaultValue = "20")
    int maxRequestsPerWindow;

    @Context
    HttpServerRequest vertxRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        boolean guarded = requestContext.getMethod().equals("POST")
                && (path.equals("api/rooms") || path.matches("api/rooms/[^/]+/join"));
        if (!guarded) {
            return;
        }

        String clientKey = clientKey();
        long windowMillis = Duration.ofSeconds(windowSeconds).toMillis();
        long now = System.currentTimeMillis();

        Window window = windows.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStartMillis() >= windowMillis) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (window.count().get() > maxRequestsPerWindow) {
            LOG.warnf("Rate limit exceeded for %s on %s", clientKey, path);
            requestContext.abortWith(Response.status(429)
                    .entity(Map.of("code", "RATE_LIMITED", "message", "Too many requests, please slow down."))
                    .header("Retry-After", windowSeconds)
                    .build());
        }
    }

    private String clientKey() {
        return vertxRequest != null ? vertxRequest.remoteAddress().host() : "unknown";
    }
}
