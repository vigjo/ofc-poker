package poker.ofc.room.ws;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import poker.ofc.room.RoomErrorCode;
import poker.ofc.room.RoomException;
import poker.ofc.room.RoomService;
import poker.ofc.room.auth.GuestPrincipal;
import poker.ofc.room.auth.GuestTokenService;

/**
 * Pushes room state to every connected seat whenever it changes. Game moves
 * themselves are submitted over REST (so the same validation, idempotency,
 * and error-mapping code path is reused); this socket is purely for
 * low-latency, server-to-client fan-out plus presence/heartbeat.
 */
@WebSocket(path = "/ws/rooms/{roomId}")
public class GameSocket {

    private static final Logger LOG = Logger.getLogger(GameSocket.class);
    private static final TypedKey<String> PLAYER_ID = RoomBroadcaster.PLAYER_ID;

    @Inject
    WebSocketConnection connection;

    @Inject
    RoomBroadcaster broadcaster;

    @Inject
    RoomService roomService;

    @Inject
    GuestTokenService tokenService;

    @OnOpen
    public void onOpen() {
        String roomId = connection.pathParam("roomId");
        GuestPrincipal principal = authenticate(roomId);
        connection.userData().put(PLAYER_ID, principal.playerId());
        roomService.markConnected(roomId, principal.playerId());
        broadcaster.broadcastState(roomId);
    }

    @OnTextMessage
    public void onMessage(String message) {
        // Clients may send lightweight "ping" heartbeats; nothing to do beyond
        // keeping the connection (and the Fargate task's idle-connection
        // detector) alive. Actual moves go through the REST API.
    }

    @OnClose
    public void onClose() {
        String roomId = connection.pathParam("roomId");
        String playerId = connection.userData().get(PLAYER_ID);
        if (playerId != null) {
            roomService.markDisconnected(roomId, playerId);
            broadcaster.broadcastState(roomId);
        }
    }

    @OnError
    public void onError(RoomException e) {
        LOG.warnf("Room error on socket %s: %s", connection.pathParam("roomId"), e.getMessage());
        connection.closeAndAwait();
    }

    @OnError
    public void onError(Throwable t) {
        LOG.errorf(t, "Unexpected WebSocket error on room %s", connection.pathParam("roomId"));
    }

    private GuestPrincipal authenticate(String roomId) {
        String query = connection.handshakeRequest().query();
        String token = extractQueryParam(query, "token");
        GuestPrincipal principal = tokenService.verify(token);
        if (!principal.roomId().equals(roomId)) {
            throw new RoomException(RoomErrorCode.INVALID_TOKEN, "Seat token is not valid for this room");
        }
        return principal;
    }

    private String extractQueryParam(String query, String key) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                return java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
