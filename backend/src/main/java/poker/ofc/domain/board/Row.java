package poker.ofc.domain.board;

public enum Row {
    TOP(3),
    MIDDLE(5),
    BOTTOM(5);

    private final int capacity;

    Row(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }
}
