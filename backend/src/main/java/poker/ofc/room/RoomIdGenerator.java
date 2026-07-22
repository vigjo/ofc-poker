package poker.ofc.room;

import java.security.SecureRandom;

/** Generates short, hard-to-guess codes used as both the room id and its shareable invite code. */
public final class RoomIdGenerator {

    // Excludes visually ambiguous characters (0/O, 1/I/L).
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RoomIdGenerator() {
    }

    public static String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
