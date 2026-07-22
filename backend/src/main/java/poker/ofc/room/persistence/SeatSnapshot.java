package poker.ofc.room.persistence;

public record SeatSnapshot(int seatIndex, String playerId, String displayName, boolean host, boolean connected) {
}
