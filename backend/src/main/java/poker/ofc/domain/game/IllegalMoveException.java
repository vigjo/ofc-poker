package poker.ofc.domain.game;

/** Thrown when a submitted move violates the rules of Pineapple OFC. */
public class IllegalMoveException extends RuntimeException {
    public IllegalMoveException(String message) {
        super(message);
    }
}
