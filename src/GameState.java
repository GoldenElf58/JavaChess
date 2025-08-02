import java.util.ArrayList;
import java.util.HashMap;

public class GameState {

    static private final int[] startBoard = {
            -4, -2, -3, -5, -6, -3, -2, -4,
            -1, -1, -1, -1, -1, -1, -1, -1,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1, 1, 1, 1, 1,
            4, 2, 3, 5, 6, 3, 2, 4
    };
    private final int[] board;
    private final boolean whiteQueen;
    private final boolean whiteKing;
    private final boolean blackQueen;
    private final boolean blackKing;
    private final int[] lastMove;
    private final int halfMoves;
    private final int halfMoveClock;
    private final boolean whiteMove;
    private final int color;
    private final HashMap<int[], Integer> previousPositionCount;
    private ArrayList<int[]> moves;

    GameState() {
        board = startBoard;
        whiteQueen = true;
        whiteKing = true;
        blackQueen = true;
        blackKing = true;
        lastMove = null;
        halfMoves = 0;
        halfMoveClock = 0;
        whiteMove = true;
        color = 1;
        previousPositionCount = new HashMap<>();
    }

    GameState(int[] board, boolean whiteQueen, boolean whiteKing, boolean blackQueen,
              boolean blackKing, int[] lastMove, int halfMoves, int halfMoveClock,
              boolean whiteMove, HashMap<int[], Integer> previousPositionCount) {
        this.board = board;
        this.whiteQueen = whiteQueen;
        this.whiteKing = whiteKing;
        this.blackQueen = blackQueen;
        this.blackKing = blackKing;
        this.lastMove = lastMove;
        this.halfMoves = halfMoves;
        this.halfMoveClock = halfMoveClock;
        this.whiteMove = whiteMove;
        this.color = whiteMove ? 1 : -1;
        this.previousPositionCount = previousPositionCount;
    }

    public int[] getBoard() {
        return board;
    }

    public ArrayList<int[]> getMoves() {
        if (moves != null) return moves;
        moves = getMovesNoCheck();
        return moves;
    }

    public ArrayList<int[]> getMovesNoCheck() {
        moves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (board[i] * color <= 0) continue;
            switch (board[i]) {
                case 1:
                    addMovesForPawn(moves, i);
                    break;
                case 2:
                    addMovesForKnight(moves, i);
                    break;
                case 3:
                    addMovesForBishop(moves, i);
                    break;
                case 4:
                    addMovesForRook(moves, i);
                    break;
                case 5:
                    addMovesForQueen(moves, i);
                    break;
                case 6:
                    addMovesForKing(moves, i);
                    break;
            }
        }
        return moves;
    }

    private void addMovesForKing(ArrayList<int[]> moves, int i) {
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                int[] move = {i, i + j * 8 + k};
                if (i % 8 + k == move[1] % 8 && 0 <= move[1] && move[1] < 64 &&
                        board[move[1]] * color <= 0) {
                    moves.add(move);
                }
            }
        }

        if ((whiteMove ? whiteKing : blackKing) &&
                board[i + 1] == 0 && board[i + 2] == 0 && board[i + 3] == 4 * color) {
            moves.add(new int[]{-1, i, 2});
        }

        if (((whiteMove ? whiteQueen : blackQueen)) &&
                board[i - 1] == 0 && board[i - 2] == 0 && board[i - 3] == 0 && board[i - 4] == 4 * color) {
            moves.add(new int[]{-1, i, -2});
        }
    }

    private void addSlidingMoves(ArrayList<int[]> moves, int i, int[] direction) {
        for (int j = 1; j < 8; j++) {
            int[] move = {i, i + direction[0] * j + direction[1] * j * 8};
            if (!(0 <= move[1] && move[1] < 64 && move[1] % 8 == i % 8 + direction[0] * j &&
                    move[1] / 8 == i / 8 + direction[1] * j * 8))
                break;
            if (board[move[1]] * color == 0) {
                moves.add(move);
                continue;
            }
            if (board[move[1]] * color < 0) {
                moves.add(move);
            }
            break;
        }
    }

    private void addMovesForQueen(ArrayList<int[]> moves, int i) {
        addSlidingMoves(moves, i, new int[]{1, 1});
        addSlidingMoves(moves, i, new int[]{1, -1});
        addSlidingMoves(moves, i, new int[]{-1, 1});
        addSlidingMoves(moves, i, new int[]{-1, -1});
        addSlidingMoves(moves, i, new int[]{1, 0});
        addSlidingMoves(moves, i, new int[]{-1, 0});
        addSlidingMoves(moves, i, new int[]{0, 1});
        addSlidingMoves(moves, i, new int[]{0, -1});
    }

    private void addMovesForRook(ArrayList<int[]> moves, int i) {
        addSlidingMoves(moves, i, new int[]{1, 0});
        addSlidingMoves(moves, i, new int[]{-1, 0});
        addSlidingMoves(moves, i, new int[]{0, 1});
        addSlidingMoves(moves, i, new int[]{0, -1});
    }

    private void addMovesForBishop(ArrayList<int[]> moves, int i) {
        addSlidingMoves(moves, i, new int[]{1, 1});
        addSlidingMoves(moves, i, new int[]{1, -1});
        addSlidingMoves(moves, i, new int[]{-1, 1});
        addSlidingMoves(moves, i, new int[]{-1, -1});
    }

    private void addMovesForKnight(ArrayList<int[]> moves, int i) {
        for (int j = -2; j <= 2; j += 4) {
            for (int k = -1; k <= 1; k += 2) {
                int[] move = {i, i + j * 8 + k};
                if (i % 8 + k == move[1] % 8 && 0 <= move[1] && move[1] < 64) {
                    if (board[move[1]] * color <= 0) {
                        moves.add(move);
                    }
                }
                move[1] = i + j + k * 8;
                if (i % 8 + j == move[1] % 8 && 0 <= move[1] && move[1] < 64) {
                    if (board[move[1]] * color <= 0) {
                        moves.add(move);
                    }
                }
            }
        }
    }

    private void addMovesForPawn(ArrayList<int[]> moves, int i) {
        int destination = i - 8 * color;
        boolean isPromotion = whiteMove ? i / 8 == 1 : i / 8 == 6;

        if (board[destination] == 0) {
            if (isPromotion) {
                moves.add(new int[]{-2, i, destination, 2 * color}); // Knight
                moves.add(new int[]{-2, i, destination, 3 * color}); // Bishop
                moves.add(new int[]{-2, i, destination, 4 * color}); // Rook
                moves.add(new int[]{-2, i, destination, 5 * color}); // Queen
            } else {
                moves.add(new int[]{i, destination}); // normal move
                if ((whiteMove ? i / 8 == 6 : i / 8 == 1) && board[i - 16] == 0) {
                    moves.add(new int[]{i, i - 16 * color}); // double move
                }
            }
        }

        // Capture Left
        if ((destination - 1) % 8 != 7 && board[destination - 1] * color < 0) {
            if (isPromotion) {
                moves.add(new int[]{-2, i, destination - 1, 2 * color}); // Knight
                moves.add(new int[]{-2, i, destination - 1, 3 * color}); // Bishop
                moves.add(new int[]{-2, i, destination - 1, 4 * color}); // Rook
                moves.add(new int[]{-2, i, destination - 1, 5 * color}); // Queen
            } else {
                moves.add(new int[]{i, destination - 1});
            }
        }

        // Capture Right
        if ((destination + 1) % 8 != 0 && board[destination + 1] * color < 0) {
            if (isPromotion) {
                moves.add(new int[]{-2, i, destination + 1, 2 * color}); // Knight
                moves.add(new int[]{-2, i, destination + 1, 3 * color}); // Bishop
                moves.add(new int[]{-2, i, destination + 1, 4 * color}); // Rook
                moves.add(new int[]{-2, i, destination + 1, 5 * color}); // Queen
            } else {
                moves.add(new int[]{i, destination + 1});
            }
        }

        // En Passant
        if (lastMove != null) {
            // Left
            if (lastMove[1] % 8 == i % 8 - 1) {
                moves.add(new int[]{-3, i, -1});
            } else if (lastMove[1] % 8 == i % 8 + 1) { // Right
                moves.add(new int[]{-3, i, 1});
            }
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                result.append(board[i * 8 + j]).append((board[i * 8 + j] >= 0 ? "  " : " "));
            }
            result.append("\n");
        }
        return result.toString();
    }
}
