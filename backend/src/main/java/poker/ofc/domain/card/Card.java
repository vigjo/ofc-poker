package poker.ofc.domain.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * An immutable playing card. Serializes to/from a compact two-character code
 * such as {@code "Ah"} (ace of hearts) or {@code "Tc"} (ten of clubs) so the
 * wire format stays small and human-readable in logs and WebSocket frames.
 */
@Schema(type = org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING,
        pattern = "^[2-9TJQKA][cdhs]$", example = "Ah",
        description = "Two-character card code: rank (2-9,T,J,Q,K,A) + suit (c,d,h,s)")
public record Card(Rank rank, Suit suit) implements Comparable<Card> {

    @JsonValue
    public String code() {
        return rank.symbol() + suit.symbol();
    }

    @JsonCreator
    public static Card fromCode(String code) {
        if (code == null || code.length() != 2) {
            throw new IllegalArgumentException("Invalid card code: " + code);
        }
        Rank rank = rankFromSymbol(code.substring(0, 1));
        Suit suit = suitFromSymbol(code.substring(1, 2));
        return new Card(rank, suit);
    }

    private static Rank rankFromSymbol(String symbol) {
        for (Rank rank : Rank.values()) {
            if (rank.symbol().equalsIgnoreCase(symbol)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Unknown rank symbol: " + symbol);
    }

    private static Suit suitFromSymbol(String symbol) {
        for (Suit suit : Suit.values()) {
            if (suit.symbol().equalsIgnoreCase(symbol)) {
                return suit;
            }
        }
        throw new IllegalArgumentException("Unknown suit symbol: " + symbol);
    }

    @Override
    public int compareTo(Card other) {
        return Integer.compare(this.rank.value(), other.rank.value());
    }

    @Override
    public String toString() {
        return code();
    }
}
