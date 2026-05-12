import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoveGenTests {

    private static long countMoves(GameState state, int depth) {
        state.computeMoves();
        if (depth == 1) return state.getMoveCount();
        long moves = 0;
        for (int i = 0; i < state.getMoveCount(); i++) {
            moves += countMoves(state.makeMove(i), depth - 1);
        }
        return moves;
    }

    private static long countMoves(MutableGameState state, int depth) {
        state.computeMoves();
        if (depth == 1) return state.getMoveCount();
        long moves = 0;
        MutableGameState next = new GameState().asMutable();
        for (int i = 0; i < state.getMoveCount(); i++) {
            moves += countMoves(state.loadMoveTo(next, i), depth - 1);
            state.undoMove();
        }
        return moves;
    }

    @Test
    public void testA() {
        byte[] board = new byte[]{
                -4, -2, -3, -5, 0, -6, 0, -4,
                -1, -1, 0, 1, -3, -1, -1, -1,
                0, 0, -1, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 3, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                1, 1, 1, 0, 2, -2, 1, 1,
                4, 2, 3, 5, 6, 0, 0, 4
        };
        GameState state = new GameState(board);
        long moves = countMoves(state, 4);
//        assertEquals(89941194, moves); // 5 ply
        assertEquals(2103487, moves); // 4 ply
        moves = countMoves(state.asMutable(), 4);
//        assertEquals(89941194, moves); // 5 ply
        assertEquals(2103487, moves); // 4 ply
    }

    @Test
    public void testB() {
        GameState state = new GameState();
        long moves = countMoves(state, 5);
//        assertEquals(119060324, moves);
        assertEquals(4865609, moves); // 5 ply
        moves = countMoves(state.asMutable(), 5);
//        assertEquals(119060324, moves); // 6 ply
        assertEquals(4865609, moves); // 5 ply
    }
}
