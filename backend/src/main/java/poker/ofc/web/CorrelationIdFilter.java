package poker.ofc.web;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logmanager.MDC;

import java.util.UUID;

/**
 * Assigns every request a correlation id - reusing the caller's
 * {@code X-Request-Id} if present, otherwise minting one - so a single move
 * or room action can be traced across the access log, application logs, and
 * (once returned to the client) any bug report. The id is echoed back on the
 * response and pushed into the logging MDC so JSON log lines automatically
 * carry it.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String incoming = requestContext.getHeaderString(HEADER);
        String requestId = (incoming != null && !incoming.isBlank()) ? incoming.trim() : UUID.randomUUID().toString();
        requestContext.setProperty(MDC_KEY, requestId);
        MDC.put(MDC_KEY, requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object requestId = requestContext.getProperty(MDC_KEY);
        if (requestId != null) {
            responseContext.getHeaders().putSingle(HEADER, requestId.toString());
        }
        MDC.remove(MDC_KEY);
    }
}
