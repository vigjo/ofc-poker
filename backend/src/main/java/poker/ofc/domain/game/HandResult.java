package poker.ofc.domain.game;

import java.util.List;

public record HandResult(List<PlayerResult> players) {

    public PlayerResult playerResult(String playerId) {
        return players.stream()
                .filter(p -> p.playerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + playerId));
    }
}
