package poker.ofc.domain.card;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckTest {

    @Test
    void shuffledDeckContains52UniqueCards() {
        Deck deck = Deck.shuffled(new Random(42));
        Set<Card> seen = new HashSet<>();
        int remaining = deck.remaining();
        List<Card> all = deck.deal(remaining);
        assertEquals(52, all.size());
        seen.addAll(all);
        assertEquals(52, seen.size(), "deck must not contain duplicate cards");
    }

    @Test
    void dealReducesRemainingCount() {
        Deck deck = Deck.shuffled(new Random(1));
        assertEquals(52, deck.remaining());
        deck.deal(5);
        assertEquals(47, deck.remaining());
        deck.deal(12);
        assertEquals(35, deck.remaining());
    }

    @Test
    void sameSeedProducesSameOrder() {
        Deck a = Deck.shuffled(new Random(7));
        Deck b = Deck.shuffled(new Random(7));
        assertEquals(a.deal(52), b.deal(52));
    }

    @Test
    void dealingMoreThanRemainingThrows() {
        Deck deck = Deck.shuffled(new Random(2));
        deck.deal(50);
        assertThrows(IllegalStateException.class, () -> deck.deal(3));
    }

    @Test
    void cardCodeRoundTrips() {
        Card aceOfSpades = new Card(Rank.ACE, Suit.SPADES);
        assertEquals("As", aceOfSpades.code());
        assertEquals(aceOfSpades, Card.fromCode("As"));

        Card tenOfClubs = new Card(Rank.TEN, Suit.CLUBS);
        assertEquals("Tc", tenOfClubs.code());
        assertEquals(tenOfClubs, Card.fromCode("Tc"));
        assertTrue(Card.fromCode("tc").equals(tenOfClubs), "codes should parse case-insensitively");
    }
}
