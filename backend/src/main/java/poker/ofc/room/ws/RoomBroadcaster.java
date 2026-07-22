package poker.ofc.room.ws;

import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import poker.ofc.room.Room;
import poker.ofc.room.RoomException;
import poker.ofc.room.RoomRegistry;
import poker.ofc.room.RoomViewMapper;
import poker.ofc.room.api.RoomStateView;

/**
 * Pushes a fresh, player-filtered {@link RoomStateView} to every open
 * WebSocket connection for a room. Called by {@link GameSocket} on
 * connect/disconnect and by {@code RoomService} after every REST-driven
 * mutation, so all connected clients converge on the same state regardless
 * of which transport triggered the change.
 */
@ApplicationScoped
public class RoomBroadcaster {

    static final TypedKey<String> PLAYER_ID = TypedKey.forString("playerId");

    private static final Logger LOG = Logger.getLogger(RoomBroadcaster.class);

    @Inject
    OpenConnections openConnections;

    @Inject
    RoomRegistry registry;

    @Inject
    RoomViewMapper viewMapper;

    public void broadcastState(String roomId) {
        Room room;
        try {
            room = registry.get(roomId);
        } catch (RoomException e) {
            return;
        }
        for (WebSocketConnection open : openConnections.listAll()) {
            if (!roomId.equals(open.pathParam("roomId"))) {
                continue;
            }
            String playerId = open.userData().get(PLAYER_ID);
            if (playerId == null) {
                continue;
            }
            RoomStateView view = viewMapper.viewFor(room, playerId);
            open.sendTextAndAwait(view);
        }
    }
}
