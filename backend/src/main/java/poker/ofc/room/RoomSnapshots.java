package poker.ofc.room;

import poker.ofc.domain.board.Board;
import poker.ofc.domain.board.Row;
import poker.ofc.domain.game.PineappleHand;
import poker.ofc.domain.game.PlayerBoard;
import poker.ofc.room.persistence.HandSnapshot;
import poker.ofc.room.persistence.PlayerBoardSnapshot;
import poker.ofc.room.persistence.RoomSnapshot;
import poker.ofc.room.persistence.SeatSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts between the live, mutable {@link Room} aggregate and its persisted snapshot form. */
public final class RoomSnapshots {

    private RoomSnapshots() {
    }

    public static RoomSnapshot toSnapshot(Room room) {
        List<SeatSnapshot> seats = room.seats().stream()
                .map(seat -> new SeatSnapshot(seat.seatIndex(), seat.playerId(), seat.displayName(), seat.isHost(), seat.isConnected()))
                .toList();

        HandSnapshot handSnapshot = null;
        PineappleHand hand = room.currentHand();
        if (hand != null) {
            Map<String, PlayerBoardSnapshot> boards = new LinkedHashMap<>();
            for (String playerId : hand.seatOrder()) {
                PlayerBoard board = hand.boardOf(playerId);
                boards.put(playerId, new PlayerBoardSnapshot(
                        board.isFantasyland(),
                        board.board().cards(Row.TOP),
                        board.board().cards(Row.MIDDLE),
                        board.board().cards(Row.BOTTOM),
                        board.pendingCards()));
            }
            handSnapshot = new HandSnapshot(hand.seatOrder(), hand.activeOrder(), hand.street(), hand.turnPointer(),
                    hand.remainingDeckCards(), boards);
        }

        return new RoomSnapshot(room.roomId(), room.inviteCode(), room.version(), room.status(), seats, handSnapshot,
                room.lastHandResult(), room.fantasylandNextHand(), room.runningTotals(), room.createdAt(), room.lastActivityAt());
    }

    public static Room fromSnapshot(RoomSnapshot snapshot) {
        List<Seat> seats = snapshot.seats().stream()
                .map(s -> {
                    Seat seat = new Seat(s.seatIndex(), s.playerId(), s.displayName(), s.host());
                    if (!s.connected()) {
                        seat.markDisconnected();
                    }
                    return seat;
                })
                .toList();

        PineappleHand hand = null;
        HandSnapshot handSnapshot = snapshot.hand();
        if (handSnapshot != null) {
            Map<String, PlayerBoard> boards = new LinkedHashMap<>();
            handSnapshot.boards().forEach((playerId, boardSnapshot) -> {
                Board board = Board.restore(boardSnapshot.top(), boardSnapshot.middle(), boardSnapshot.bottom());
                boards.put(playerId, new PlayerBoard(playerId, boardSnapshot.fantasyland(), board, boardSnapshot.pendingCards()));
            });
            hand = PineappleHand.restore(handSnapshot.seatOrder(), handSnapshot.activeOrder(), boards,
                    handSnapshot.remainingDeck(), handSnapshot.street(), handSnapshot.turnPointer());
        }

        return Room.restore(snapshot.roomId(), snapshot.inviteCode(), seats, snapshot.status(), hand,
                snapshot.lastHandResult(), snapshot.fantasylandNextHand(), snapshot.runningTotals(), snapshot.version(),
                snapshot.createdAt(), snapshot.lastActivityAt());
    }
}
