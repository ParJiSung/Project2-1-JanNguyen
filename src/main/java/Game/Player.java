package Game;

public enum Player {
    RED(1, Color.RED),
    BLACK(2, Color.BLACK);

    public final int id;
    public final Color stone;

    Player(int id, Color stone) { this.id = id; this.stone = stone; }

    public Player other() { return this == RED ? BLACK : RED; }

    public static Player fromId(int id) { return id == 1 ? RED : BLACK; }
}


