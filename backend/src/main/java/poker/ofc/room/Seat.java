package poker.ofc.room;

import java.time.Instant;

/** One seat at a room's table. Mutable: connection state changes over the room's lifetime. */
public final class Seat {

    private final int seatIndex;
    private final String playerId;
    private final String displayName;
    private final boolean host;
    private boolean connected;
    private Instant lastSeenAt;

    public Seat(int seatIndex, String playerId, String displayName, boolean host) {
        this.seatIndex = seatIndex;
        this.playerId = playerId;
        this.displayName = displayName;
        this.host = host;
        this.connected = true;
        this.lastSeenAt = Instant.now();
    }

    public int seatIndex() {
        return seatIndex;
    }

    public String playerId() {
        return playerId;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isHost() {
        return host;
    }

    public boolean isConnected() {
        return connected;
    }

    public Instant lastSeenAt() {
        return lastSeenAt;
    }

    public void markConnected() {
        this.connected = true;
        this.lastSeenAt = Instant.now();
    }

    public void markDisconnected() {
        this.connected = false;
        this.lastSeenAt = Instant.now();
    }
}
