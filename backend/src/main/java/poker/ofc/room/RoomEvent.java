package poker.ofc.room;

import java.time.Instant;
import java.util.Map;

/**
 * A lightweight, replayable record of one accepted room transition. Used to
 * push incremental updates over the WebSocket and to fast-forward a
 * reconnecting client that only missed a few versions.
 */
public record RoomEvent(long version, RoomEventType type, Map<String, Object> payload, Instant occurredAt) {
}
