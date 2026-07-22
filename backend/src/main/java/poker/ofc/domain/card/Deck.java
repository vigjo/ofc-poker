package poker.ofc.domain.card;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A 52-card deck. The server is the sole authority for shuffling and dealing;
 * clients never see undealt cards. Use {@link #shuffled(Random)} with an
 * injected {@link Random} for deterministic tests, and {@link #shuffled()}
 * (backed by {@link SecureRandom}) everywhere else, including production.
 */
public final class Deck {

    private final List<Card> cards;

    private Deck(List<Card> cards) {
        this.cards = cards;
    }

    public static Deck shuffled() {
        return shuffled(new SecureRandom());
    }

    public static Deck shuffled(Random random) {
        List<Card> fresh = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                fresh.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(fresh, random);
        return new Deck(fresh);
    }

    /** Rebuilds a deck from a previously persisted list of remaining cards, in order. */
    public static Deck fromRemaining(List<Card> remaining) {
        return new Deck(new ArrayList<>(remaining));
    }

    /** A read-only view of the cards still left to deal, for persistence/snapshotting. */
    public List<Card> peekRemaining() {
        return List.copyOf(cards);
    }

    /** Removes and returns the next {@code count} cards from the top of the deck. */
    public List<Card> deal(int count) {
        if (count > cards.size()) {
            throw new IllegalStateException(
                    "Cannot deal " + count + " cards, only " + cards.size() + " remain");
        }
        List<Card> dealt = new ArrayList<>(cards.subList(0, count));
        cards.subList(0, count).clear();
        return dealt;
    }

    public int remaining() {
        return cards.size();
    }
}
