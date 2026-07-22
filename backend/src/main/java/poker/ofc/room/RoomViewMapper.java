package poker.ofc.room;

import jakarta.enterprise.context.ApplicationScoped;
import poker.ofc.domain.board.Row;
import poker.ofc.domain.card.Card;
import poker.ofc.domain.game.PlayerBoard;
import poker.ofc.room.api.PlayerView;
import poker.ofc.room.api.RoomStateView;

import java.util.List;

/** Builds the player-filtered {@link RoomStateView} sent over REST and WebSocket. */
@ApplicationScoped
public class RoomViewMapper {

    public RoomStateView viewFor(Room room, String viewerPlayerId) {
        List<PlayerView> players = room.seats().stream()
                .map(seat -> toPlayerView(room, seat, viewerPlayerId))
                .toList();

        String currentActor = room.currentHand() != null ? room.currentHand().currentActor() : null;
        var street = room.currentHand() != null ? room.currentHand().street() : null;

        return new RoomStateView(room.roomId(), room.inviteCode(), room.version(), room.status(), viewerPlayerId,
                currentActor, street, players, room.lastHandResult(), room.runningTotals());
    }

    private PlayerView toPlayerView(Room room, Seat seat, String viewerPlayerId) {
        boolean isYou = seat.playerId().equals(viewerPlayerId);
        List<Card> top = List.of();
        List<Card> middle = List.of();
        List<Card> bottom = List.of();
        int pendingCount = 0;
        List<Card> pendingCards = null;
        boolean fantasyland = false;

        if (room.currentHand() != null) {
            try {
                PlayerBoard board = room.currentHand().boardOf(seat.playerId());
                top = board.board().cards(Row.TOP);
                middle = board.board().cards(Row.MIDDLE);
                bottom = board.board().cards(Row.BOTTOM);
                pendingCount = board.pendingCards().size();
                fantasyland = board.isFantasyland();
                if (isYou) {
                    pendingCards = board.pendingCards();
                }
            } catch (IllegalArgumentException notInThisHand) {
                // Player joined after the current hand started; nothing to show yet.
            }
        }

        return new PlayerView(seat.playerId(), seat.displayName(), seat.seatIndex(), seat.isHost(), seat.isConnected(),
                fantasyland, top, middle, bottom, pendingCount, pendingCards, isYou);
    }
}
