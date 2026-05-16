import game.GameState;
import game.MutableGameState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoveGenTests {

    public static final MutableGameState[] gameStates = new MutableGameState[6];

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
        if (gameStates[depth - 1] == null) gameStates[depth - 1] = new GameState().asMutable();
        MutableGameState tmp = gameStates[depth - 1];
        for (int i = 0; i < state.getMoveCount(); i++) {
            moves += countMoves(state.loadMoveTo(tmp, i), depth - 1);
            state.undoMove();
        }
        return moves;
    }

    @Test
    public void benchmarkMutable() {
        GameState state = new GameState();

        countMoves(state.asMutable(), 6);

        long start = System.nanoTime();
        long nodes = countMoves(state.asMutable(), 6);
        long end = System.nanoTime();

        double sec = (end - start) / 1e9;

        IO.println("====== Mutable ======");
        IO.println("Nodes: " + nodes);
        IO.println("Time: " + sec);
        IO.println("MNPS: " + ((int) (nodes / sec)) / 1_000_000.0);
    }

    @Test
    public void benchmarkImmutable() {
        GameState state = new GameState();

        countMoves(state, 6);

        long start = System.nanoTime();
        long nodes = countMoves(state, 6);
        long end = System.nanoTime();

        double sec = (end - start) / 1e9;

        IO.println("===== Immutable =====");
        IO.println("Nodes: " + nodes);
        IO.println("Time: " + sec);
        IO.println("MNPS: " + ((int) (nodes / sec)) / 1_000_000.0);
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
