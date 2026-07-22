package poker.ofc.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Adds baseline security headers to every REST response. This is a JSON-only
 * API with no server-rendered HTML, so the policy is deliberately locked
 * down: no framing, no MIME sniffing, no referrer leakage, and a
 * default-deny content security policy.
 */
@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @ConfigProperty(name = "ofc.security.hsts-enabled", defaultValue = "false")
    boolean hstsEnabled;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var headers = responseContext.getHeaders();
        headers.putSingle("X-Content-Type-Options", "nosniff");
        headers.putSingle("X-Frame-Options", "DENY");
        headers.putSingle("Referrer-Policy", "no-referrer");
        headers.putSingle("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
        headers.putSingle("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        headers.putSingle("Cache-Control", "no-store");
        if (hstsEnabled) {
            headers.putSingle("Strict-Transport-Security", "max-age=63072000; includeSubDomains");
        }
    }
}
