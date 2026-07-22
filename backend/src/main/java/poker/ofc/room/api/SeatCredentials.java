package poker.ofc.room.api;

/** Returned once, on create/join, so the client can authenticate later REST calls and the WebSocket. */
public record SeatCredentials(String roomId, String inviteCode, String playerId, String displayName, String seatToken) {
}
