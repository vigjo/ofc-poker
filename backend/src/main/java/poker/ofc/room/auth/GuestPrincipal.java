package poker.ofc.room.auth;

/** The identity carried by a signed guest seat token. */
public record GuestPrincipal(String roomId, String playerId, String displayName) {
}
