package AI.mcts.HexGame;

import java.util.Objects;

public class Move {
    public final int row;
    public final int col;

    public Move(int row, int col){
        this.row = row;
        this.col = col;
    }

    public String getCoordinate(){
        return "(" + row + "," + col + ")";
    }

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (!(o instanceof Move)) {
            return false;
        }
        Move m = (Move) o;
        return row == m.row && col == m.col;
    }
    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString(){
        return getCoordinate();
    }
}
