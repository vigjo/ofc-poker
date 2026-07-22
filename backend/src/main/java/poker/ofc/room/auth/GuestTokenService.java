package poker.ofc.room.auth;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import poker.ofc.room.RoomErrorCode;
import poker.ofc.room.RoomException;

import java.time.Duration;

/**
 * Issues and verifies short-lived, room-scoped guest seat tokens. A token
 * authorizes its holder to act as one specific seat in one specific room; it
 * is not a general-purpose account credential.
 */
@ApplicationScoped
public class GuestTokenService {

    @Inject
    JWTParser parser;

    @ConfigProperty(name = "ofc.auth.token-ttl-minutes")
    long tokenTtlMinutes;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public String issue(String roomId, String playerId, String displayName) {
        return Jwt.claims()
                .issuer(issuer)
                .subject(playerId)
                .claim("roomId", roomId)
                .claim("displayName", displayName)
                .expiresIn(Duration.ofMinutes(tokenTtlMinutes))
                .sign();
    }

    public GuestPrincipal verify(String token) {
        if (token == null || token.isBlank()) {
            throw new RoomException(RoomErrorCode.INVALID_TOKEN, "Missing seat token");
        }
        try {
            JsonWebToken jwt = parser.parse(token);
            String roomId = jwt.getClaim("roomId");
            String displayName = jwt.getClaim("displayName");
            if (roomId == null || jwt.getSubject() == null) {
                throw new RoomException(RoomErrorCode.INVALID_TOKEN, "Seat token is missing required claims");
            }
            return new GuestPrincipal(roomId, jwt.getSubject(), displayName);
        } catch (ParseException e) {
            throw new RoomException(RoomErrorCode.INVALID_TOKEN, "Seat token is invalid or expired");
        }
    }
}
