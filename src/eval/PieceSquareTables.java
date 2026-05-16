package eval;

import static java.lang.Math.abs;

public class PieceSquareTables {
    /**
     * Get the value of a piece on a square
     *
     * @param piece   Piece as an integer: 0=none, 1=pawn, 2=knight, 3=bishop, 4=rook, 5=queen,
     *                6=king, negative piece for black.
     * @param square  Square of the piece as an integer. Top left is 0, bottom right is 63.
     * @return The score of the piece in its position.
     */
    public static int getPieceSquareValue(int piece, int square) {
        if (piece == 0) return 0;
        int[] pieceTable = switch (piece) {
            case -1, 1 -> Pawns;
            case -2, 2 -> Knights;
            case -3, 3 -> Bishops;
            case -4, 4 -> Rooks;
            case -5, 5 -> Queens;
            case -6, 6 -> KingStart;
            default -> throw new IllegalArgumentException("Invalid piece: " + piece);
        };
        return piece > 0 ? pieceTable[square] : -pieceTable[63 - square];
    }

    public static int getPieceSquareValueEndgame(int piece, int square) {
        if (piece == 0) return 0;
        int[] pieceTable = switch (piece) {
            case -1, 1 -> PawnsEnd;
            case -2, 2 -> Knights;
            case -3, 3 -> Bishops;
            case -4, 4 -> Rooks;
            case -5, 5 -> Queens;
            case -6, 6 -> KingEnd;
            default -> throw new IllegalArgumentException("Invalid piece: " + piece);
        };
        return piece > 0 ? pieceTable[square] : -pieceTable[63 - square];
    }

    public static int convertToKingEndgame(byte[] board, int curEval) {
        byte piece;
        int eval = curEval;
        for (int i = 0; i < 64; i++) {
            piece = board[i];
            if (abs(piece) == 6) {
                eval -= getPieceSquareValue(piece, i);
                eval += getPieceSquareValueEndgame(piece, i);
            }
        }
        return eval;
    }

    public static int convertToEndgame(byte[] board, int curEval) {
        byte piece;
        int eval = curEval;
        for (int i = 0; i < 64; i++) {
            piece = board[i];
            if (abs(piece) == 1 || abs(piece) == 6) {
                eval -= getPieceSquareValue(piece, i);
                eval += getPieceSquareValueEndgame(piece, i);
            }
        }
        return eval;
    }

    public static final int[] Pawns = {
            100, 100, 100, 100, 100, 100, 100, 100,
            150, 150, 150, 150, 150, 150, 150, 150,
            110, 110, 120, 130, 130, 120, 110, 110,
            105, 105, 110, 125, 125, 110, 105, 105,
            100, 100, 100, 120, 120, 100, 100, 100,
            105, 95, 90, 100, 100, 90, 95, 105,
            105, 110, 110, 80, 80, 110, 110, 105,
            100, 100, 100, 100, 100, 100, 100, 100
    };

    public static final int[] PawnsEnd = {
            100, 100, 100, 100, 100, 100, 100, 100,
            180, 180, 180, 180, 180, 180, 180, 180,
            150, 150, 150, 150, 150, 150, 150, 150,
            130, 130, 130, 130, 130, 130, 130, 130,
            120, 120, 120, 120, 120, 120, 120, 120,
            110, 110, 110, 110, 110, 110, 110, 110,
            110, 110, 110, 110, 110, 110, 110, 110,
            100, 100, 100, 100, 100, 100, 100, 100
    };

    public static final int[] Rooks = {
            500, 500, 500, 500, 500, 500, 500, 500,
            505, 510, 510, 510, 510, 510, 510, 505,
            495, 500, 500, 500, 500, 500, 500, 495,
            495, 500, 500, 500, 500, 500, 500, 495,
            495, 500, 500, 500, 500, 500, 500, 495,
            495, 500, 500, 500, 500, 500, 500, 495,
            495, 500, 500, 500, 500, 500, 500, 495,
            500, 500, 500, 505, 505, 500, 500, 500
    };
    public static final int[] Knights = {
            250, 260, 270, 270, 270, 270, 260, 250,
            260, 280, 300, 300, 300, 300, 280, 260,
            270, 300, 310, 315, 315, 310, 300, 270,
            270, 305, 315, 320, 320, 315, 305, 270,
            270, 300, 315, 320, 320, 315, 300, 270,
            270, 305, 310, 315, 315, 310, 305, 270,
            260, 280, 300, 305, 305, 300, 280, 260,
            250, 260, 270, 270, 270, 270, 260, 250
    };
    public static final int[] Bishops = {
            280, 290, 290, 290, 290, 290, 290, 280,
            290, 300, 300, 300, 300, 300, 300, 290,
            290, 300, 305, 310, 310, 305, 300, 290,
            290, 305, 305, 310, 310, 305, 305, 290,
            290, 300, 310, 310, 310, 310, 300, 290,
            290, 310, 310, 310, 310, 310, 310, 290,
            290, 305, 300, 300, 300, 300, 305, 290,
            280, 290, 290, 290, 290, 290, 290, 280
    };
    public static final int[] Queens = {
            880, 890, 890, 895, 895, 890, 890, 880,
            890, 900, 900, 900, 900, 900, 900, 890,
            890, 900, 905, 905, 905, 905, 900, 890,
            895, 900, 905, 905, 905, 905, 900, 895,
            900, 900, 905, 905, 905, 905, 900, 895,
            890, 905, 905, 905, 905, 905, 900, 890,
            890, 900, 905, 900, 900, 900, 900, 890,
            880, 890, 890, 895, 895, 890, 890, 880
    };
    public static final int[] KingStart = {
            -80, -70, -70, -70, -70, -70, -70, -80,
            -60, -60, -60, -60, -60, -60, -60, -60,
            -40, -50, -50, -60, -60, -50, -50, -40,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, -5, -5, -5, -5, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20
    };

    public static final int[] KingEnd = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -5, 0, 5, 5, 5, 5, 0, -5,
            -10, -5, 20, 30, 30, 20, -5, -10,
            -15, -10, 35, 45, 45, 35, -10, -15,
            -20, -15, 30, 40, 40, 30, -15, -20,
            -25, -20, 20, 25, 25, 20, -20, -25,
            -30, -25, 0, 0, 0, 0, -25, -30,
            -50, -30, -30, -30, -30, -30, -30, -50
    };
}
