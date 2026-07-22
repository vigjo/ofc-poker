package poker.ofc.room;

/** A room/lobby-level error, as opposed to an in-game rules violation ({@code IllegalMoveException}). */
public class RoomException extends RuntimeException {

    private final RoomErrorCode code;

    public RoomException(RoomErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public RoomErrorCode code() {
        return code;
    }
}
