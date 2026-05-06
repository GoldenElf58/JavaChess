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

    public long hash(int idx, byte piece) {
        return zobristTable[idx][piece + 6];
    }

    public long hash(byte[] board) {
        long hash = 0;
        for (int i = 0; i < 64; i++) {
            byte piece = board[i];
            if (piece == 0) continue;
            hash ^= hash(i, piece);
        }
        return hash;
    }

    public long hash(boolean whiteQueen, boolean whiteKing, boolean blackQueen,
                     boolean blackKing, boolean isWhiteMove) {
        long hash = 0;
        if (whiteQueen) hash ^= zobristTable[64][1];
        if (whiteKing) hash ^= zobristTable[64][0];
        if (blackQueen) hash ^= zobristTable[64][3];
        if (blackKing) hash ^= zobristTable[64][2];
        if (isWhiteMove) hash ^= zobristTable[64][4];
        return hash;
    }

    public long hash(byte[] board, boolean whiteQueen, boolean whiteKing, boolean blackQueen,
                     boolean blackKing, boolean isWhiteMove) {
        return hash(board) ^ hash(whiteQueen, whiteKing, blackQueen, blackKing, isWhiteMove);
    }

    public long hash(GameState state) {
        return hash(state.getBoard(), state.whiteQueen, state.whiteKing, state.blackQueen,
                state.blackKing, state.isWhiteMove());
    }
}
