import java.util.HashMap;

import static java.lang.Math.abs;

public class GameState {

    private static final int MAX_MOVES = 256;

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
    public final boolean whiteQueen;
    public final boolean whiteKing;
    public final boolean blackQueen;
    public final boolean blackKing;
    private final int[] lastMove;
    private final int halfMoves;
    private final int halfMoveClock;
    private final boolean whiteMove;
    private final int color;
    private final HashMap<int[], Integer> previousPositionCount;
    private final int[] moves = new int[MAX_MOVES * 4];
    private int moveCount = 0;

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

    public int[] getMoves() {
        if (moveCount == 0) {
            computeMoves();
        }
        return moves;
    }

    private void computeMoves() {
        moveCount = 0;
        int pieceType;
        for (int i = 0; i < 64; i++) {
            pieceType = board[i] * color;
            if (pieceType <= 0) continue;
            switch (pieceType) {
                case 1:
                    addMovesForPawn(i);
                    break;
                case 2:
                    addMovesForKnight(i);
                    break;
                case 3:
                    addMovesForBishop(i);
                    break;
                case 4:
                    addMovesForRook(i);
                    break;
                case 5:
                    addMovesForQueen(i);
                    break;
                case 6:
                    addMovesForKing(i);
                    break;
            }
        }
    }

    private void addMoveSlot(int a, int b) {
        moves[moveCount * 4] = a;
        moves[moveCount * 4 + 1] = b;
        moveCount++;
    }

    private void addMoveSlot(int a, int b, int c) {
        moves[moveCount * 4] = a;
        moves[moveCount * 4 + 1] = b;
        moves[moveCount * 4 + 2] = c;
        moveCount++;
    }

    private void addMoveSlot(int a, int b, int c, int d) {
        moves[moveCount * 4] = a;
        moves[moveCount * 4 + 1] = b;
        moves[moveCount * 4 + 2] = c;
        moves[moveCount * 4 + 3] = d;
        moveCount++;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public int[] getMove(int moveIdx) {
        return new int[]{moves[moveIdx * 4], moves[moveIdx * 4 + 1], moves[moveIdx * 4 + 2], moves[moveIdx * 4 + 3]};
    }

    public GameState makeMove(int[] move) {
        int[] newBoard = board.clone();
        boolean whiteQueen = this.whiteQueen;
        boolean whiteKing = this.whiteKing;
        boolean blackQueen = this.blackQueen;
        boolean blackKing = this.blackKing;

        // Castle
        if (move[0] == -1) {
            newBoard[move[1] + move[2] * 2] = 6 * color;
            newBoard[move[1] + move[2]] = 4 * color;
            newBoard[move[1]] = 0;
            newBoard[move[1] + (move[2] == 1 ? 3 : -4)] = 0;
            if (whiteMove) {
                whiteQueen = false;
                whiteKing = false;
            } else {
                blackQueen = false;
                blackKing = false;
            }
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, halfMoveClock + 1,
                    !whiteMove, new HashMap<>());
        }

        // Promotion
        if (move[0] == -2) {
            newBoard[move[2]] = move[3];
            newBoard[move[1]] = 0;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, 0,
                    !whiteMove, new HashMap<>());
        }

        // En Passant
        if (move[0] == -3) {
            newBoard[move[1] - color * 8 + move[2]] = color;
            newBoard[move[1] + move[2]] = 0;
            newBoard[move[1]] = 0;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, 0,
                    !whiteMove, new HashMap<>());
        }

        int piece = newBoard[move[0]];
        int captured = newBoard[move[1]];
        newBoard[move[0]] = 0;
        newBoard[move[1]] = piece;
        if (piece == -6) {
            blackQueen = false;
            blackKing = false;
        } else if (piece == 6) {
            whiteQueen = false;
            whiteKing = false;
        } else if (piece == -4) {
            if (move[0] == 0) blackQueen = false;
            else if (move[0] == 7) blackKing = false;
        } else if (piece == 4) {
            if (move[0] == 56) whiteQueen = false;
            else if (move[0] == 63) whiteKing = false;
        }

        if (captured != 0) {
            if (captured == -4) {
                if (move[1] == 0) blackQueen = false;
                else if (move[1] == 7) blackKing = false;
            } else if (captured == 4) {
                if (move[1] == 56) whiteQueen = false;
                else if (move[1] == 63) whiteKing = false;
            }
        }

        if (piece == 1 && abs(move[0] - move[1]) == 16) {
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move, halfMoves + 1, 0,
                    !whiteMove, new HashMap<>());
        }

        int halfMoveClock = piece == 1 ? 0 : (captured == 0 ? this.halfMoveClock + 1 : 0);

        if (halfMoveClock > 0) {
            HashMap<int[], Integer> previousPositionCount = new HashMap<>(this.previousPositionCount);
            int positionCount = previousPositionCount.getOrDefault(this.board, 0) + 1;
            previousPositionCount.put(this.board, positionCount);
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, halfMoveClock,
                    !whiteMove, previousPositionCount);
        }

        return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                null, halfMoves + 1, 0,
                !whiteMove, new HashMap<>());
    }

    public boolean isWinner() {
        boolean hasWhiteKing = false;
        boolean hasBlackKing = false;
        for (int piece : board) {
            if (piece == 6) {
                hasWhiteKing = true;
                if (hasBlackKing) break;
            } else if (piece == -6) {
                hasBlackKing = true;
                if (hasWhiteKing) break;
            }
        }
        return !hasWhiteKing || !hasBlackKing;
    }

    private void addMovesForKing(int i) {
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                int[] move = {i, i + j * 8 + k};
                if (i % 8 + k == move[1] % 8 && 0 <= move[1] && move[1] < 64 &&
                        board[move[1]] * color <= 0) {
                    addMoveSlot(i, i + j * 8 + k);
                }
            }
        }

        if ((whiteMove ? whiteKing : blackKing) &&
                board[i + 1] == 0 && board[i + 2] == 0 && board[i + 3] == 4 * color) {
            addMoveSlot(-1, i, 1);
        }

        if (((whiteMove ? whiteQueen : blackQueen)) &&
                board[i - 1] == 0 && board[i - 2] == 0 && board[i - 3] == 0 && board[i - 4] == 4 * color) {
            addMoveSlot(-1, i, -1);
        }
    }

    private void addSlidingMoves(int i, int[] direction) {
        for (int j = 1; j < 8; j++) {
            int[] move = {i, i + direction[0] * j + direction[1] * j * 8};
            if (!(0 <= move[1] && move[1] < 64 && move[1] % 8 == i % 8 + direction[0] * j &&
                    move[1] / 8 == i / 8 + direction[1] * j))
                break;
            int targetPieceType = board[move[1]] * color;
            if (targetPieceType == 0) {
                addMoveSlot(i, move[1]);
                continue;
            }
            if (targetPieceType < 0) {
                addMoveSlot(i, move[1]);
            }
            break;
        }
    }

    private void addMovesForQueen(int i) {
        addSlidingMoves(i, new int[]{1, 1});
        addSlidingMoves(i, new int[]{1, -1});
        addSlidingMoves(i, new int[]{-1, 1});
        addSlidingMoves(i, new int[]{-1, -1});
        addSlidingMoves(i, new int[]{1, 0});
        addSlidingMoves(i, new int[]{-1, 0});
        addSlidingMoves(i, new int[]{0, 1});
        addSlidingMoves(i, new int[]{0, -1});
    }

    private void addMovesForRook(int i) {
        addSlidingMoves(i, new int[]{1, 0});
        addSlidingMoves(i, new int[]{-1, 0});
        addSlidingMoves(i, new int[]{0, 1});
        addSlidingMoves(i, new int[]{0, -1});
    }

    private void addMovesForBishop(int i) {
        addSlidingMoves(i, new int[]{1, 1});
        addSlidingMoves(i, new int[]{1, -1});
        addSlidingMoves(i, new int[]{-1, 1});
        addSlidingMoves(i, new int[]{-1, -1});
    }

    private void addMovesForKnight(int i) {
        for (int j = -2; j <= 2; j += 4) {
            for (int k = -1; k <= 1; k += 2) {
                int[] move = {i, i + j * 8 + k};
                if (i % 8 + k == move[1] % 8 && 0 <= move[1] && move[1] < 64) {
                    if (board[move[1]] * color <= 0) {
                        addMoveSlot(i, move[1]);
                    }
                }
                move = new int[]{i, i + j + k * 8};
                if (i % 8 + j == move[1] % 8 && 0 <= move[1] && move[1] < 64) {
                    if (board[move[1]] * color <= 0) {
                        addMoveSlot(i, move[1]);
                    }
                }
            }
        }
    }

    private void addMovesForPawn(int i) {
        int destination = i - 8 * color;
        boolean isPromotion = whiteMove ? i / 8 == 1 : i / 8 == 6;

        if (board[destination] == 0) {
            if (isPromotion) {
                addMoveSlot(-2, i, destination, 2 * color); // Knight
                addMoveSlot(-2, i, destination, 3 * color); // Bishop
                addMoveSlot(-2, i, destination, 4 * color); // Rook
                addMoveSlot(-2, i, destination, 5 * color); // Queen
            } else {
                addMoveSlot(i, destination); // normal move
                if ((whiteMove ? i / 8 == 6 : i / 8 == 1) && board[i - 16 * color] == 0) {
                    addMoveSlot(i, i - 16 * color); // double move
                }
            }
        }

        // Capture Left
        if ((destination - 1 + 8) % 8 != 7 && board[destination - 1] * color < 0) {
            if (isPromotion) {
                addMoveSlot(-2, i, destination - 1, 2 * color); // Knight
                addMoveSlot(-2, i, destination - 1, 3 * color); // Bishop
                addMoveSlot(-2, i, destination - 1, 4 * color); // Rook
                addMoveSlot(-2, i, destination - 1, 5 * color); // Queen
            } else {
                addMoveSlot(i, destination - 1);
            }
        }

        // Capture Right
        if ((destination + 1) % 8 != 0 && board[destination + 1] * color < 0) {
            if (isPromotion) {
                addMoveSlot(-2, i, destination + 1, 2 * color); // Knight
                addMoveSlot(-2, i, destination + 1, 3 * color); // Bishop
                addMoveSlot(-2, i, destination + 1, 4 * color); // Rook
                addMoveSlot(-2, i, destination + 1, 5 * color); // Queen
            } else {
                addMoveSlot(i, destination + 1);
            }
        }

        // En Passant
        if (lastMove != null) {
            // Left
            if (lastMove[1] % 8 == i % 8 - 1 && lastMove[1] == i - 1) {
                addMoveSlot(-3, i, -1);
            } else if (lastMove[1] % 8 == i % 8 + 1 && lastMove[1] == i + 1) { // Right
                addMoveSlot(-3, i, 1);
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
