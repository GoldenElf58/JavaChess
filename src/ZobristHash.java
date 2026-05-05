import java.util.Random;

public class ZobristHash {

    private final long[][] zobristTable = new long[65][13];

    public ZobristHash() {
        Random random = new Random();
        for (int i = 0; i < 65; i++) {
            for (int j = 0; j < 13; j++) {
                zobristTable[i][j] = random.nextLong();
            }
        }
    }

    public long hash(byte[] board) {
        long hash = 0;
        for (int i = 0; i < 64; i++) {
            int piece = board[i];
            if (piece == 0) continue;
            hash ^= zobristTable[i][piece + 6];
        }
        return hash;
    }

    public long hash(byte[] board, boolean whiteKing, boolean whiteQueen, boolean blackKing,
                     boolean blackQueen, boolean isWhiteMove) {
        long hash = hash(board);
        if (whiteKing) hash ^= zobristTable[64][0];
        if (whiteQueen) hash ^= zobristTable[64][1];
        if (blackKing) hash ^= zobristTable[64][2];
        if (blackQueen) hash ^= zobristTable[64][3];
        if (isWhiteMove) hash ^= zobristTable[64][4];
        return hash;
    }

    public long hash(GameState state) {
        return hash(state.getBoard(), state.whiteKing, state.whiteQueen, state.blackKing,
                state.blackQueen, state.isWhiteMove());
    }

    public long hash(MutableGameState state) {
        return hash(state.getBoard(), state.whiteKing, state.whiteQueen, state.blackKing,
                state.blackQueen, state.isWhiteMove());
    }
}
