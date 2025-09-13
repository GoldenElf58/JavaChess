import java.util.Arrays;

import static java.lang.Math.abs;

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
    public final boolean whiteQueen;
    public final boolean whiteKing;
    public final boolean blackQueen;
    public final boolean blackKing;
    private final int[] lastMove;
    private final int halfMoves;
    private final int halfMoveClock;
    private final boolean whiteMove;
    private final int color;
    private final PositionHistory positionHistory;
    private int[] moves;
    private int moveCount = 0;
    private boolean movesGenerated = false;
    private boolean isWinner;
    private int winner;
    private final int blackKnights;
    private final int whiteKnights;
    private final int blackBishops;
    private final int whiteBishops;
    private final int otherPieces;

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
        positionHistory = new PositionHistory(Arrays.hashCode(board));
        isWinner = false;
        winner = 0;
        blackKnights = 2;
        whiteKnights = 2;
        blackBishops = 2;
        whiteBishops = 2;
        otherPieces = 22;
    }

    GameState(int[] board, boolean whiteQueen, boolean whiteKing, boolean blackQueen,
              boolean blackKing, int[] lastMove, int halfMoves, int halfMoveClock,
              boolean whiteMove, PositionHistory positionHistory, boolean isWinner,
              int winner, int blackKnights, int whiteKnights, int blackBishops, int whiteBishops,
              int otherPieces) {
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
        this.positionHistory = positionHistory;
        this.isWinner = isWinner;
        this.winner = winner;
        this.blackKnights = blackKnights;
        this.whiteKnights = whiteKnights;
        this.blackBishops = blackBishops;
        this.whiteBishops = whiteBishops;
        this.otherPieces = otherPieces;
    }

    public void computeMoves() {
        if (moveCount != 0) return;
        computeMovesPseudoLegal();
        int[] board = this.board;
        int pieceTaken;
        boolean illegal;
        int[] newMoves = new int[moveCount * 3];
        int newMoveCount = 0;
        int[] move;
        int currKingIdx = getKingIdx();
        int kingIdx;
        boolean kingMoved;
        if (currKingIdx == -1) return;
        boolean inCheck = inCheckByNonSlidingPiece(currKingIdx);
        for (int moveIdx = 0; moveIdx < moveCount; moveIdx++) {
            move = getMove(moveIdx);
            pieceTaken = makeMoveOnlyBoard(move);
            illegal = false;
            if (move[0] == -1) {
                int throughIdx = currKingIdx + move[2];
                kingIdx = throughIdx + move[2];
                for (int i = 0; i < 64; i++) {
                    switch (board[i] * color) {
                        case 0:
                            break;
                        case -1:
                            illegal = isPawnAttacking(i, kingIdx, currKingIdx, throughIdx);
                            break;
                        case -2:
                            illegal = isKnightAttacking(i, kingIdx, currKingIdx, throughIdx);
                            break;
                        case -3:
                            illegal = isBishopAttacking(i, kingIdx, currKingIdx, throughIdx, board);
                            break;
                        case -4:
                            illegal = isRookAttacking(i, kingIdx, currKingIdx, throughIdx, board);
                            break;
                        case -5:
                            illegal = isQueenAttacking(i, kingIdx, currKingIdx, throughIdx, board);
                            break;
                        case -6:
                            illegal = isKingAttacking(i, kingIdx, currKingIdx, throughIdx);
                            break;
                    }
                    if (illegal) break;
                }
            } else {
                kingMoved = move[0] >= 0 && move[2] == 6;
                if (inCheck && !kingMoved) illegal = true;
                kingIdx = kingMoved ? move[1] : currKingIdx;
                for (int i = 0; i < 64 && !illegal; i++) {
                    switch (board[i] * color) { // make board local
                        case 0:
                            break;
                        case -1:
                            if (kingMoved) illegal = isPawnAttacking(i, kingIdx);
                            break;
                        case -2:
                            if (kingMoved) illegal = isKnightAttacking(i, kingIdx);
                            break;
                        case -3:
                            illegal = isBishopAttacking(i, kingIdx);
                            break;
                        case -4:
                            illegal = isRookAttacking(i, kingIdx);
                            break;
                        case -5:
                            illegal = isQueenAttacking(i, kingIdx);
                            break;
                        case -6:
                            if (kingMoved) illegal = isKingAttacking(i, kingIdx);
                            break;
                    }
                }
            }
            undoMoveOnlyBoard(move, pieceTaken);
            if (!illegal) {
                newMoves[newMoveCount * 3] = move[0];
                newMoves[newMoveCount * 3 + 1] = move[1];
                newMoves[newMoveCount * 3 + 2] = move[2];
                newMoveCount++;
            }
        }
        moves = newMoves;
        movesGenerated = true;
        moveCount = newMoveCount;
    }

    private int getKingIdx() {
        for (int i = 0; i < 64; i++) {
            if (board[i] == 6 * color) {
                return i;
            }
        }
        return -1;
    }

    private boolean inCheckByNonSlidingPiece(int kingIdx) {
        boolean inCheck;
        for (int i = 0; i < 64; i++) {
            inCheck = switch (board[i] * color) {
                case -1 -> isPawnAttacking(i, kingIdx);
                case -2 -> isKnightAttacking(i, kingIdx);
                case -6 -> isKingAttacking(i, kingIdx);
                default -> false;
            };
            if (inCheck) return true;
        }
        return false;
    }

    public boolean inCheck() {
        return inCheck(getKingIdx());
    }

    private boolean inCheck(int kingIdx) {
        boolean inCheck;
        for (int i = 0; i < 64; i++) {
            inCheck = switch (board[i] * color) {
                case -1 -> isPawnAttacking(i, kingIdx);
                case -2 -> isKnightAttacking(i, kingIdx);
                case -3 -> isBishopAttacking(i, kingIdx);
                case -4 -> isRookAttacking(i, kingIdx);
                case -5 -> isQueenAttacking(i, kingIdx);
                case -6 -> isKingAttacking(i, kingIdx);
                default -> false;
            };
            if (inCheck) return true;
        }
        return false;
    }

    private boolean isPawnAttacking(int pawnIdx, int targetIdx) {
        int forwardSquare = pawnIdx + 8 * color;

        return (forwardSquare - 1 == targetIdx && ((forwardSquare - 1) & 7) != 7) ||
                (forwardSquare + 1 == targetIdx && ((forwardSquare + 1) & 7) != 0);
    }

    private boolean isKnightAttacking(int knightIdx, int targetIdx) {
        int knightMod8 = knightIdx & 7;
        for (int j = -2; j <= 2; j += 4) {
            for (int k = -1; k <= 1; k += 2) {
                int target = knightIdx + j * 8 + k;
                if (knightMod8 + k == (target & 7) && target == targetIdx) return true;
                target = knightIdx + j + k * 8;
                if (knightMod8 + j == (target & 7) && target == targetIdx) return true;
            }
        }
        return false;
    }

    private boolean isDiagonalSlidingPieceAttacking(int pieceIdx, int col, int row,
                                                    int targetIdx, int d1, int d2) {
        for (int j = 1; j < 8; j++) {
            col += d1;
            row += d2;
            if (col < 0 || col > 7 || row < 0 || row > 7) break;
            pieceIdx += d1 + d2 * 8;
            if (pieceIdx == targetIdx) return true;
            if (pieceIdx < 0 || pieceIdx > 63 || board[pieceIdx] != 0) break;
        }
        return false;
    }

    private boolean isFileSlidingPieceAttacking(int pieceIdx, int row, int targetIdx, int dir) {
        for (int j = 1; j < 8; j++) {
            row += dir;
            if (row < 0 || row > 7) break;
            pieceIdx += dir * 8;
            if (pieceIdx == targetIdx) return true;
            if (board[pieceIdx] != 0) break;
        }
        return false;
    }

    private boolean isRankSlidingPieceAttacking(int pieceIdx, int col, int targetIdx, int dir) {
        for (int j = 1; j < 8; j++) {
            col += dir;
            if (col < 0 || col > 7) break;
            pieceIdx += dir;
            if (pieceIdx == targetIdx) return true;
            if (board[pieceIdx] != 0) break;
        }
        return false;
    }

    private boolean isBishopAttacking(int bishopIdx, int targetIdx) {
        int bishopMod8 = bishopIdx & 7;
        int bishopDiv8 = bishopIdx / 8;
        return isDiagonalSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx, 1, 1)
                || isDiagonalSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx, 1, -1)
                || isDiagonalSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx, -1, 1)
                || isDiagonalSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx, -1, -1);
    }

    private boolean isRookAttacking(int rookIdx, int targetIdx) {
        int rookMod8 = rookIdx & 7;
        int rookDiv8 = rookIdx / 8;
        return isFileSlidingPieceAttacking(rookIdx, rookDiv8, targetIdx, 1)
                || isFileSlidingPieceAttacking(rookIdx, rookDiv8, targetIdx, -1)
                || isRankSlidingPieceAttacking(rookIdx, rookMod8, targetIdx, 1)
                || isRankSlidingPieceAttacking(rookIdx, rookMod8, targetIdx, -1);
    }

    private boolean isQueenAttacking(int queenIdx, int targetIdx) {
        int queenMod8 = queenIdx & 7;
        int queenDiv8 = queenIdx / 8;
        return isDiagonalSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, 1, 1)
                || isDiagonalSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, 1, -1)
                || isDiagonalSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, -1, 1)
                || isDiagonalSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, -1, -1)
                || isFileSlidingPieceAttacking(queenIdx, queenDiv8, targetIdx, 1)
                || isFileSlidingPieceAttacking(queenIdx, queenDiv8, targetIdx, -1)
                || isRankSlidingPieceAttacking(queenIdx, queenMod8, targetIdx, 1)
                || isRankSlidingPieceAttacking(queenIdx, queenMod8, targetIdx, -1);
    }

    private boolean isKingAttacking(int kingIdx, int targetIdx1, int targetIdx2, int targetIdx3) {
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                int destination = kingIdx + j * 8 + k;
                if ((destination == targetIdx1 || destination == targetIdx2
                        || destination == targetIdx3) && (kingIdx & 7) + k == (destination & 7))
                    return true;
            }
        }
        return false;
    }

    private boolean isPawnAttacking(int pawnIdx, int targetIdx1, int targetIdx2, int targetIdx3) {
        int forwardSquare = pawnIdx + 8 * color;

        return ((forwardSquare - 1 == targetIdx1 || forwardSquare - 1 == targetIdx2 ||
                forwardSquare - 1 == targetIdx3) && ((forwardSquare - 1) & 7) != 7) ||
                ((forwardSquare + 1 == targetIdx1 || forwardSquare + 1 == targetIdx2 ||
                        forwardSquare + 1 == targetIdx3) && ((forwardSquare + 1) & 7) != 0);
    }

    private boolean isKnightAttacking(int knightIdx, int targetIdx1, int targetIdx2, int targetIdx3) {
        int knightMod8 = knightIdx & 7;
        for (int j = -2; j <= 2; j += 4) {
            for (int k = -1; k <= 1; k += 2) {
                int target = knightIdx + j * 8 + k;
                if (knightMod8 + k == (target & 7) && (target == targetIdx1
                        || target == targetIdx2 || target == targetIdx3)) return true;
                target = knightIdx + j + k * 8;
                if (knightMod8 + j == (target & 7) && (target == targetIdx1
                        || target == targetIdx2 || target == targetIdx3)) return true;
            }
        }
        return false;
    }

    private boolean isBishopAttacking(int bishopIdx, int targetIdx1, int targetIdx2, int targetIdx3, int[] board) {
        int bishopMod8 = bishopIdx & 7;
        int bishopDiv8 = bishopIdx / 8;
        for (int d1 = -1; d1 <= 1; d1 += 2) {
            for (int d2 = -1; d2 <= 1; d2 += 2) {
                for (int j = 1; j < 8; j++) {
                    int target = bishopIdx + d1 * j + d2 * j * 8;
                    if (!((target & 7) == bishopMod8 + d1 * j && target / 8 == bishopDiv8 + d2 * j))
                        break;
                    if (target == targetIdx1 || target == targetIdx2 || target == targetIdx3)
                        return true;
                    if (target < 0 || target > 63 || board[target] != 0) break;
                }
            }
        }
        return false;
    }

    private boolean isRookAttacking(int rookIdx, int targetIdx1, int targetIdx2, int targetIdx3,
                                    int[] board) {
        int rookMod8 = rookIdx & 7;
        int rookDiv8 = rookIdx / 8;
        for (int d1 = -1; d1 <= 1; d1 += 2) {
            for (int j = 1; j < 8; j++) {
                int target = rookIdx + d1 * j;
                if (!((target & 7) == rookMod8 + d1 * j && target / 8 == rookDiv8))
                    break;
                if (target == targetIdx1 || target == targetIdx2 || target == targetIdx3)
                    return true;
                if (board[target] != 0) break;
            }
        }
        for (int d2 = -1; d2 <= 1; d2 += 2) {
            for (int j = 1; j < 8; j++) {
                int target = rookIdx + d2 * j * 8;
                if (!((target & 7) == rookMod8 && target / 8 == rookDiv8 + d2 * j))
                    break;
                if (target == targetIdx1 || target == targetIdx2 || target == targetIdx3)
                    return true;
                if (target < 0 || target > 63 || board[target] != 0) break;
            }
        }
        return false;
    }

    private boolean isQueenAttacking(int queenIdx, int targetIdx1, int targetIdx2, int targetIdx3,
                                     int[] board) {
        int queenMod8 = queenIdx & 7;
        int queenDiv8 = queenIdx / 8;
        for (int d1 = -1; d1 <= 1; d1++) {
            for (int d2 = -1; d2 <= 1; d2++) {
                if (d1 == 0 && d2 == 0) continue;
                for (int j = 1; j < 8; j++) {
                    int target = queenIdx + d1 * j + d2 * j * 8;
                    if (!((target & 7) == queenMod8 + d1 * j && target / 8 == queenDiv8 + d2 * j))
                        break;
                    if (target == targetIdx1 || target == targetIdx2 || target == targetIdx3)
                        return true;
                    if (target < 0 || target > 63 || board[target] != 0) break;
                }
            }
        }
        return false;
    }

    private boolean isKingAttacking(int kingIdx, int targetIdx) {
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                int destination = kingIdx + j * 8 + k;
                if (destination == targetIdx && (kingIdx & 7) + k == (destination & 7))
                    return true;
            }
        }
        return false;
    }

    public void computeMovesPseudoLegal() {
        if (moves == null) moves = new int[654]; // 218 * 3
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
        moves[moveCount * 3] = a;
        moves[moveCount * 3 + 1] = b;
        moveCount++;
    }

    private void addMoveSlot(int a, int b, int c) {
        moves[moveCount * 3] = a;
        moves[moveCount * 3 + 1] = b;
        moves[moveCount * 3 + 2] = c;
        moveCount++;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public int[] getMove(int moveIdx) {
        return new int[]{moves[moveIdx * 3], moves[moveIdx * 3 + 1], moves[moveIdx * 3 + 2]};
    }

    public GameState makeMove(int moveIdx) {
        return makeMove(getMove(moveIdx));
    }

    public GameState makeMove(int[] move) {
        int[] newBoard = board.clone();
        boolean whiteQueen = this.whiteQueen;
        boolean whiteKing = this.whiteKing;
        boolean blackQueen = this.blackQueen;
        boolean blackKing = this.blackKing;
        int blackKnights = this.blackKnights;
        int whiteKnights = this.whiteKnights;
        int whiteBishops = this.whiteBishops;
        int blackBishops = this.blackBishops;
        int otherPieces = this.otherPieces;

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
                    null, halfMoves + 1, halfMoveClock + 1, !whiteMove,
                    new PositionHistory(Arrays.hashCode(newBoard)), false, 0, blackKnights,
                    whiteKnights, blackBishops, whiteBishops, otherPieces);
        }

        // Promotion
        if (move[0] == -2) {
            newBoard[move[1] - 8 * color] = move[2];
            otherPieces--;
            if (move[2] == 2) whiteKnights++;
            else if (move[2] == 3) whiteBishops++;
            else if (move[2] == -2) blackKnights++;
            else if (move[2] == -3) blackBishops++;
            else otherPieces++;
            newBoard[move[1]] = 0;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, 0, !whiteMove,
                    new PositionHistory(Arrays.hashCode(newBoard)), false, 0, blackKnights,
                    whiteKnights, blackBishops, whiteBishops, otherPieces);
        }

        // En Passant
        if (move[0] == -3) {
            newBoard[move[1] - color * 8 + move[2]] = color;
            newBoard[move[1] + move[2]] = 0;
            newBoard[move[1]] = 0;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, halfMoveClock + 1, !whiteMove,
                    new PositionHistory(Arrays.hashCode(newBoard)), false, 0, blackKnights,
                    whiteKnights, blackBishops, whiteBishops, otherPieces - 1);
        }

        // Promotion Taking
        if (move[0] <= -4) {
            newBoard[move[2]] = (-2 - move[0]) * color;
            newBoard[move[1]] = 0;
            otherPieces--;
            if (board[move[2]] == 2) whiteKnights--;
            else if (board[move[2]] == 3) whiteBishops--;
            else if (board[move[2]] == -2) blackKnights--;
            else if (board[move[2]] == -3) blackBishops--;
            else otherPieces--;
            if (move[0] == -4) {
                if (color == 1) whiteKnights++;
                else blackKnights++;
            }
            else if (move[0] == -5) {
                if (color == 1) whiteBishops++;
                else blackBishops++;
            }
            else otherPieces++;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, halfMoveClock + 1, !whiteMove,
                    new PositionHistory(Arrays.hashCode(newBoard)), false, 0, blackKnights,
                    whiteKnights, blackBishops, whiteBishops, otherPieces - 1);
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
                otherPieces--;
                if (move[1] == 0) blackQueen = false;
                else if (move[1] == 7) blackKing = false;
            } else if (captured == 4) {
                otherPieces--;
                if (move[1] == 56) whiteQueen = false;
                else if (move[1] == 63) whiteKing = false;
            } else if (captured == 2) whiteKnights--;
            else if (captured == 3) whiteBishops--;
            else if (captured == -2) blackKnights--;
            else if (captured == -3) blackBishops--;
            else otherPieces--;
        } else if ((piece == 1 || piece == -1) && abs(move[0] - move[1]) == 16) {
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move, halfMoves + 1, 0,
                    !whiteMove, new PositionHistory(Arrays.hashCode(newBoard)), false, 0,
                    blackKnights, whiteKnights, blackBishops, whiteBishops, otherPieces);
        }

        int halfMoveClock = piece == 1 || captured != 0 ? 0 : this.halfMoveClock + 1;

        if (halfMoveClock > 0) {
            int boardHash = Arrays.hashCode(newBoard);
            PositionHistory positionHistory = new PositionHistory(boardHash, this.positionHistory);
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, halfMoveClock,
                    !whiteMove, positionHistory, positionHistory.count >= 3, 0, blackKnights,
                    whiteKnights, blackBishops, whiteBishops, otherPieces);
        }

        return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                null, halfMoves + 1, 0, !whiteMove, new PositionHistory(Arrays.hashCode(newBoard)),
                false, 0, blackKnights, whiteKnights, blackBishops, whiteBishops, otherPieces);
    }

    private int makeMoveOnlyBoard(int[] move) {
        // Castle
        if (move[0] == -1) {
            board[move[1] + move[2] * 2] = 6 * color;
            board[move[1] + move[2]] = 4 * color;
            board[move[1]] = 0;
            board[move[1] + (move[2] == 1 ? 3 : -4)] = 0;
            return 0;
        }

        // Promotion
        if (move[0] == -2) {
            board[move[1] - 8 * color] = move[2];
            board[move[1]] = 0;
            return 0;
        }

        // En Passant
        if (move[0] == -3) {
            board[move[1] - color * 8 + move[2]] = color;
            board[move[1] + move[2]] = 0;
            board[move[1]] = 0;
            return -color;
        }

        // Promotion Taking
        if (move[0] <= -4) {
            int pieceTaken = board[move[2]];
            board[move[2]] = -2 - move[0];
            board[move[1]] = 0;
            return pieceTaken;
        }

        int pieceTaken = board[move[1]];
        board[move[1]] = board[move[0]];
        board[move[0]] = 0;

        return pieceTaken;
    }

    private void undoMoveOnlyBoard(int[] move, int pieceTaken) {
        // Castle
        if (move[0] == -1) {
            int move_1 = move[1];
            board[move_1 + move[2] * 2] = 0;
            board[move_1 + move[2]] = 0;
            board[move_1] = 6 * color;
            board[move_1 + (move[2] == 1 ? 3 : -4)] = 4 * color;
            return;
        }

        // Promotion
        if (move[0] == -2) {
            board[move[1] - 8 * color] = 0;
            board[move[1]] = color;
            return;
        }

        // En Passant
        if (move[0] == -3) {
            board[move[1] - color * 8 + move[2]] = 0;
            board[move[1] + move[2]] = pieceTaken;
            board[move[1]] = color;
            return;
        }

        // Promotion Taking
        if (move[0] <= -4) {
            board[move[2]] = pieceTaken;
            board[move[1]] = color;
            return;
        }

        board[move[0]] = board[move[1]];
        board[move[1]] = pieceTaken;
    }

    public boolean isInProgress() {
        if (isWinner) return false;
        if (halfMoveClock >= 100) {
            winner = 0;
            return false;
        }
        if (movesGenerated && moveCount == 0) {
            winner = inCheck() ? -color : 0;
            return false;
        }
        if (otherPieces == 0) {
            int minorPieces = whiteKnights + whiteBishops + blackKnights + blackBishops;
            if (minorPieces <= 1) {
                winner = 0;
                isWinner = true;
                return false;
            }
            if (minorPieces == 2) {
                if ((whiteKnights == 1 && blackKnights == 1) || (whiteBishops + whiteKnights == 1)) {
                    isWinner = true;
                    winner = 0;
                    return false;
                }
            }
        }
        return true;
    }

    public int getWinner() {
        return winner;
    }

    public int[] getBoard() {
        return board;
    }

    public int getColor() {
        return color;
    }

    public boolean isWhiteMove() {
        return whiteMove;
    }

    private void addMovesForKing(int i) {
        int idxMod8 = i & 7;
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                int destination = i + j * 8 + k;
                if (idxMod8 + k == (destination & 7) && 0 <= destination && destination < 64 &&
                        board[destination] * color <= 0) {
                    addMoveSlot(i, destination, 6);
                }
            }
        }

        if ((whiteMove ? whiteKing : blackKing) &&
                board[i + 1] == 0 && board[i + 2] == 0 && board[i + 3] == 4 * color) {
            addMoveSlot(-1, i, 1);
        }

        if (((whiteMove ? whiteQueen : blackQueen)) && board[i - 1] == 0 && board[i - 2] == 0 &&
                board[i - 3] == 0 && board[i - 4] == 4 * color) {
            addMoveSlot(-1, i, -1);
        }
    }

    private void addSlidingMoves(int i, int direction1, int direction2) {
        int idxMod8 = i & 7;
        int idxDiv8 = i / 8;
        for (int j = 1; j < 8; j++) {
            int target = i + direction1 * j + direction2 * j * 8;
            if (!(0 <= target && target < 64 && (target & 7) == idxMod8 + direction1 * j &&
                    target / 8 == idxDiv8 + direction2 * j))
                break;
            int targetPieceType = board[target] * color;
            if (targetPieceType == 0) {
                addMoveSlot(i, target);
                continue;
            }
            if (targetPieceType < 0)
                addMoveSlot(i, target);
            break;
        }
    }

    private void addMovesForQueen(int i) {
        addSlidingMoves(i, 1, 1);
        addSlidingMoves(i, 1, -1);
        addSlidingMoves(i, -1, 1);
        addSlidingMoves(i, -1, -1);
        addSlidingMoves(i, 1, 0);
        addSlidingMoves(i, -1, 0);
        addSlidingMoves(i, 0, 1);
        addSlidingMoves(i, 0, -1);
    }

    private void addMovesForRook(int i) {
        addSlidingMoves(i, 1, 0);
        addSlidingMoves(i, -1, 0);
        addSlidingMoves(i, 0, 1);
        addSlidingMoves(i, 0, -1);
    }

    private void addMovesForBishop(int i) {
        addSlidingMoves(i, 1, 1);
        addSlidingMoves(i, 1, -1);
        addSlidingMoves(i, -1, 1);
        addSlidingMoves(i, -1, -1);
    }

    private void addMovesForKnight(int i) {
        int idxMod8 = i & 7;
        int target;
        for (int j = -2; j <= 2; j += 4) {
            for (int k = -1; k <= 1; k += 2) {
                target = i + j * 8 + k;
                if (idxMod8 + k == (target & 7) && 0 <= target && target < 64
                        && board[target] * color <= 0)
                    addMoveSlot(i, target);
                target = i + j + k * 8;
                if (idxMod8 + j == (target & 7) && 0 <= target && target < 64
                        && board[target] * color <= 0)
                    addMoveSlot(i, target);
            }
        }
    }

    private void addMovesForPawn(int i) {
        int forwardSquare = i - 8 * color;
        boolean isPromotion = whiteMove ? i / 8 == 1 : i / 8 == 6;

        if (board[forwardSquare] == 0) {
            if (isPromotion) {
                addMoveSlot(-2, i, 5 * color); // Queen
                addMoveSlot(-2, i, 4 * color); // Rook
                addMoveSlot(-2, i, 3 * color); // Bishop
                addMoveSlot(-2, i, 2 * color); // Knight
            } else {
                addMoveSlot(i, forwardSquare); // normal move
                if ((whiteMove ? i / 8 == 6 : i / 8 == 1) && board[i - 16 * color] == 0) {
                    addMoveSlot(i, i - 16 * color); // double move
                }
            }
        }

        // Capture Left
        if (((forwardSquare - 1) & 7) != 7 && board[forwardSquare - 1] * color < 0) {
            if (isPromotion) {
                addMoveSlot(-7, i, forwardSquare - 1); // Queen
                addMoveSlot(-6, i, forwardSquare - 1); // Rook
                addMoveSlot(-5, i, forwardSquare - 1); // Bishop
                addMoveSlot(-4, i, forwardSquare - 1); // Knight
            } else {
                addMoveSlot(i, forwardSquare - 1);
            }
        }

        // Capture Right
        if (((forwardSquare + 1) & 7) != 0 && board[forwardSquare + 1] * color < 0) {
            if (isPromotion) {
                addMoveSlot(-7, i, forwardSquare + 1); // Queen
                addMoveSlot(-6, i, forwardSquare + 1); // Rook
                addMoveSlot(-5, i, forwardSquare + 1); // Bishop
                addMoveSlot(-4, i, forwardSquare + 1); // Knight
            } else {
                addMoveSlot(i, forwardSquare + 1);
            }
        }

        // En Passant
        if (lastMove != null) {
            if ((lastMove[1] & 7) == (i & 7) - 1 && lastMove[1] == i - 1) { // Left
                addMoveSlot(-3, i, -1);
            } else if ((lastMove[1] & 7) == (i & 7) + 1 && lastMove[1] == i + 1) { // Right
                addMoveSlot(-3, i, 1);
            }
        }
    }

    public int[] findMove(int from, int to) {
        if (to == from || from == -1) return null;
        for (int i = 0; i < moveCount; i++) {
            int[] move = this.getMove(i);
            if (move[0] >= 0) {
                if (move[0] == from && move[1] == to) return move;
            } else {
                if (move[0] == -1) {
                    if (move[1] == from && (to == from + move[2] * 2 ||
                            (move[2] == 1 ? to == from + 3 : to == from - 4)))
                        return move;
                } else if (move[0] == -2) {
                    if (move[1] == from && from - 8 * color == to) return move;
                } else if (move[0] == -3) {
                    if (move[1] == from && to == from - 8 * color + move[2]) return move;
                } else if (move[1] == from && move[2] == to) return move;
            }
        }
        return null;
    }

    public String moveToString(int moveIdx) {
        return moveToString(getMove(moveIdx));
    }

    public String moveToString(int[] move) {
        return String.format("%c%d %c%d", 'a' + (move[0] & 7), 8 - move[0] / 8,
                'a' + (move[1] & 7), 8 - move[1] / 8);
    }

    public String moveRepr(int moveIdx) {
        return moveRepr(getMove(moveIdx));
    }

    public String moveRepr(int[] move) {
        return String.format("(%d, %d, %d)", move[0], move[1], move[2]);
    }

    public void printMoves() {
        for (int i = 0; i < moveCount; i++) {
            System.out.printf("%s | %s%n", moveRepr(i), moveToString(i));
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
