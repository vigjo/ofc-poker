# Pineapple OFC rules interpretation

Open Face Chinese poker has several house-rule variants in circulation. This
document is the single source of truth for exactly which variant this app
implements, so the rules engine, its tests, and the UI copy never drift from
each other. The implementation lives under
[`backend/src/main/java/poker/ofc/domain`](../backend/src/main/java/poker/ofc/domain);
every class there links back to the relevant section below.

## Table shape

- 2-3 players, one shared 52-card deck, no jokers.
- Each player has a 13-card board: a 3-card **top** row, a 5-card **middle**
  row, and a 5-card **bottom** row, ranked bottom > middle > top.

## Dealing and turns (Pineapple variant)

1. **Initial street**: every player is dealt 5 cards and places all 5 (no
   discard).
2. **Streets 1-4**: every player is dealt 3 cards, places 2, and discards 1
   face down (discards are never revealed to other players).
3. Players act in a fixed seat order within a street - the same order every
   street - so a player always sees every board that has acted before them
   that street, matching physical Pineapple OFC play. See
   `PineappleHand#currentActor` and `Street`.
4. **Fantasyland**: a player who qualifies (see below) is dealt all 13 cards
   at once at the start of the next hand and submits one whole-board move,
   independent of the normal turn order. See
   `PineappleHand#applyFantasylandMove`.

## Fouling

A board **fouls** if its rows are not in non-decreasing strength from top to
bottom, i.e. top must not outrank middle, and middle must not outrank bottom
(`Board#isFouled`). A fouled board:

- Scores zero royalties for the hand.
- Automatically loses all three rows (a full "scoop") against every
  opponent who did not also foul.
- **Pushes** (scores zero) against another fouled player.

## Fantasyland qualification

- **Entering**: top row is trips, or a pair of queens or better (QQ+), and
  the board did not foul (`Board#qualifiesForFantasyland`).
- **Staying in** (re-entering directly from Fantasyland without playing a
  normal street first): top row is trips, or the bottom row is a full house
  or better (`Board#qualifiesToStayInFantasyland`).

## Scoring: head-to-head "1-6" convention

Every pair of players is compared independently; a 3-player hand is scored
as three separate pairwise comparisons summed per player
(`ScoringEngine#score`).

For each pair, unless one or both boards fouled:

1. **Row points**: compare top, middle, and bottom independently. Winning a
   row is `+1`, losing is `-1`, a tie is `0`.
2. **Scoop bonus**: winning all three rows against an opponent (`+3` row
   points) adds a further `+3`; losing all three (`-3` row points) adds a
   further `-3`. Otherwise no scoop bonus.
3. **Royalty differential**: `own royalty total - opponent royalty total` is
   added regardless of who won more rows.

If exactly one board in the pair fouled, the non-fouled player scores `+3`
row points and `+3` scoop bonus against them (a full scoop), plus the
royalty differential (the fouled player's royalties are zero). If both
fouled, the pair pushes entirely (all three components are `0`).

A player's total for the hand is the sum of their pairwise totals against
every opponent (`PlayerResult#total`); running totals across hands in a room
are a plain per-player sum of hand totals (`Room#runningTotals`).

## Royalty tables

Royalties are looked up from `HandRank` per row (`Royalties`):

| Top row (3 cards)        | Points |
| ------------------------- | ------ |
| Pair, 66 through AA        | rank − 5 (1-9) |
| Trips, 222 through AAA     | rank + 8 (10-22) |

| Middle row (5 cards) | Points | Bottom row (5 cards) | Points |
| --------------------- | ------ | ---------------------- | ------ |
| Trips                  | 2      | -                       | -      |
| Straight                | 4      | Straight                | 2      |
| Flush                   | 8      | Flush                   | 4      |
| Full house              | 12     | Full house               | 6      |
| Quads                    | 20     | Quads                    | 10     |
| Straight flush           | 30     | Straight flush            | 15     |
| Royal flush              | 50     | Royal flush                | 25     |

A fouled board scores zero royalties regardless of what its rows would
otherwise have qualified for.

## Deliberately out of scope for launch

- Joker/54-card variants, "1-1-1" (Russian) scoring, and progressive
  Fantasyland variants (14/15-card Fantasyland deals) are not implemented.
- 4+ player tables are not supported (`PineappleHand.deal` rejects seat
  counts outside 2-3).
