import java.util.HashMap;

import static java.lang.Math.abs;

public class GameState {

    private static final int MAX_MOVES = 218;

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
    private int[] moves = new int[MAX_MOVES * 3];
    private int moveCount = 0;
    private boolean movesGenerated = false;
    private boolean isWinner;
    private int winner;

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
        isWinner = false;
        winner = 0;
    }

    GameState(int[] board, boolean whiteQueen, boolean whiteKing, boolean blackQueen,
              boolean blackKing, int[] lastMove, int halfMoves, int halfMoveClock,
              boolean whiteMove, HashMap<int[], Integer> previousPositionCount, boolean isWinner,
              int winner) {
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
        this.isWinner = isWinner;
        this.winner = winner;
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
                kingMoved = move[2] == 6;
                kingIdx = kingMoved ? move[1] : currKingIdx;
                for (int i = 0; i < 64; i++) {
                    switch (board[i] * color) { // make board local
                        case 0:
                            break;
                        case -1:
                            if (kingMoved || inCheck) illegal = isPawnAttacking(i, kingIdx);
                            break;
                        case -2:
                            if (kingMoved || inCheck) illegal = isKnightAttacking(i, kingIdx);
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
                            if (kingMoved || inCheck) illegal = isKingAttacking(i, kingIdx);
                            break;
                    }
                    if (illegal) break;
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

    private boolean inCheck() {
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

        return (forwardSquare - 1 == targetIdx && (forwardSquare - 1 + 8) % 8 != 7) ||
                (forwardSquare + 1 == targetIdx && (forwardSquare + 1) % 8 != 0);
    }

    private boolean isKnightAttacking(int knightIdx, int targetIdx) {
        int knightMod8 = knightIdx % 8;
        for (int j = -2; j <= 2; j += 4) {
            for (int k = -1; k <= 1; k += 2) {
                int target = knightIdx + j * 8 + k;
                if (knightMod8 + k == target % 8 && target == targetIdx) return true;
                target = knightIdx + j + k * 8;
                if (knightMod8 + j == target % 8 && target == targetIdx) return true;
            }
        }
        return false;
    }

    private boolean isSlidingPieceAttacking(int slidingIdx, int slidingMod8, int slidingDiv8,
                                            int targetIdx, int d1, int d2) {
        for (int j = 1; j < 8; j++) {
            int target = slidingIdx + d1 * j + d2 * j * 8;
            if (!(target % 8 == slidingMod8 + d1 * j && target / 8 == slidingDiv8 + d2 * j))
                break;
            if (target == targetIdx)
                return true;
            if (target < 0 || target > 63 || board[target] != 0) break;
        }
        return false;
    }

    private boolean isBishopAttacking(int bishopIdx, int targetIdx) {
        int bishopMod8 = bishopIdx % 8;
        int bishopDiv8 = bishopIdx / 8;
        return isSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx, 1, 1)
                || isSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx, 1, -1)
                || isSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx, -1, 1)
                || isSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx, -1, -1);
    }

    private boolean isRookAttacking(int rookIdx, int targetIdx) {
        int rookMod8 = rookIdx % 8;
        int rookDiv8 = rookIdx / 8;
        return isSlidingPieceAttacking(rookIdx, rookMod8, rookDiv8, targetIdx, 1, 0)
                || isSlidingPieceAttacking(rookIdx, rookMod8, rookDiv8, targetIdx, -1, 0)
                || isSlidingPieceAttacking(rookIdx, rookMod8, rookDiv8, targetIdx, 0, 1)
                || isSlidingPieceAttacking(rookIdx, rookMod8, rookDiv8, targetIdx, 0, -1);
    }

    private boolean isQueenAttacking(int queenIdx, int targetIdx) {
        int queenMod8 = queenIdx % 8;
        int queenDiv8 = queenIdx / 8;
        return isSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, 1, 1)
                || isSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, 1, -1)
                || isSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, -1, 1)
                || isSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, -1, -1)
                || isSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, 1, 0)
                || isSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, -1, 0)
                || isSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, 0, 1)
                || isSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, 0, -1);

    }

    private boolean isKingAttacking(int kingIdx, int targetIdx1, int targetIdx2, int targetIdx3) {
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                int destination = kingIdx + j * 8 + k;
                if ((destination == targetIdx1 || destination == targetIdx2
                        || destination == targetIdx3) && kingIdx % 8 + k == destination % 8)
                    return true;
            }
        }
        return false;
    }

    private boolean isPawnAttacking(int pawnIdx, int targetIdx1, int targetIdx2, int targetIdx3) {
        int forwardSquare = pawnIdx + 8 * color;

        return ((forwardSquare - 1 == targetIdx1 || forwardSquare - 1 == targetIdx2 ||
                forwardSquare - 1 == targetIdx3) && (forwardSquare - 1 + 8) % 8 != 7) ||
                ((forwardSquare + 1 == targetIdx1 || forwardSquare + 1 == targetIdx2 ||
                        forwardSquare + 1 == targetIdx3) && (forwardSquare + 1) % 8 != 0);
    }

    private boolean isKnightAttacking(int knightIdx, int targetIdx1, int targetIdx2, int targetIdx3) {
        int knightMod8 = knightIdx % 8;
        for (int j = -2; j <= 2; j += 4) {
            for (int k = -1; k <= 1; k += 2) {
                int target = knightIdx + j * 8 + k;
                if (knightMod8 + k == target % 8 && (target == targetIdx1
                        || target == targetIdx2 || target == targetIdx3)) return true;
                target = knightIdx + j + k * 8;
                if (knightMod8 + j == target % 8 && (target == targetIdx1
                        || target == targetIdx2 || target == targetIdx3)) return true;
            }
        }
        return false;
    }

    private boolean isBishopAttacking(int bishopIdx, int targetIdx1, int targetIdx2, int targetIdx3, int[] board) {
        int bishopMod8 = bishopIdx % 8;
        int bishopDiv8 = bishopIdx / 8;
        for (int d1 = -1; d1 <= 1; d1 += 2) {
            for (int d2 = -1; d2 <= 1; d2 += 2) {
                for (int j = 1; j < 8; j++) {
                    int target = bishopIdx + d1 * j + d2 * j * 8;
                    if (!(target % 8 == bishopMod8 + d1 * j && target / 8 == bishopDiv8 + d2 * j))
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
        int rookMod8 = rookIdx % 8;
        int rookDiv8 = rookIdx / 8;
        for (int d1 = -1; d1 <= 1; d1 += 2) {
            for (int j = 1; j < 8; j++) {
                int target = rookIdx + d1 * j;
                if (!(target % 8 == rookMod8 + d1 * j && target / 8 == rookDiv8))
                    break;
                if (target == targetIdx1 || target == targetIdx2 || target == targetIdx3)
                    return true;
                if (target < 0 || target > 63 || board[target] != 0) break;
            }
        }
        for (int d2 = -1; d2 <= 1; d2 += 2) {
            for (int j = 1; j < 8; j++) {
                int target = rookIdx + d2 * j * 8;
                if (!(target % 8 == rookMod8 && target / 8 == rookDiv8 + d2 * j))
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
        int queenMod8 = queenIdx % 8;
        int queenDiv8 = queenIdx / 8;
        for (int d1 = -1; d1 <= 1; d1++) {
            for (int d2 = -1; d2 <= 1; d2++) {
                if (d1 == 0 && d2 == 0) continue;
                for (int j = 1; j < 8; j++) {
                    int target = queenIdx + d1 * j + d2 * j * 8;
                    if (!(target % 8 == queenMod8 + d1 * j && target / 8 == queenDiv8 + d2 * j))
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
                if (destination == targetIdx && kingIdx % 8 + k == destination % 8)
                    return true;
            }
        }
        return false;
    }

    public void computeMovesPseudoLegal() {
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
                    !whiteMove, new HashMap<>(), false, 0);
        }

        // Promotion
        if (move[0] == -2) {
            newBoard[move[1] - 8 * color] = move[2];
            newBoard[move[1]] = 0;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, 0,
                    !whiteMove, new HashMap<>(), false, 0);
        }

        // En Passant
        if (move[0] == -3) {
            newBoard[move[1] - color * 8 + move[2]] = color;
            newBoard[move[1] + move[2]] = 0;
            newBoard[move[1]] = 0;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, 0,
                    !whiteMove, new HashMap<>(), false, 0);
        }

        // Promotion Taking
        if (move[0] <= -4) {
            newBoard[move[2]] = -2 - move[0];
            newBoard[move[1]] = 0;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, 0,
                    !whiteMove, new HashMap<>(), false, 0);
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
                    !whiteMove, new HashMap<>(), false, 0);
        }

        int halfMoveClock = piece == 1 ? 0 : (captured == 0 ? this.halfMoveClock + 1 : 0);

        if (halfMoveClock > 0) {
            HashMap<int[], Integer> previousPositionCount = new HashMap<>(this.previousPositionCount);
            int positionCount = previousPositionCount.getOrDefault(this.board, 0) + 1;
            previousPositionCount.put(this.board, positionCount);
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, halfMoveClock,
                    !whiteMove, previousPositionCount, positionCount >= 3, 0);
        }

        return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                null, halfMoves + 1, 0,
                !whiteMove, new HashMap<>(), false, 0);
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
            board[move[1] + move[2] * 2] = 0;
            board[move[1] + move[2]] = 0;
            board[move[1]] = 6 * color;
            board[move[1] + (move[2] == 1 ? 3 : -4)] = 4 * color;
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
        boolean hasWhiteKing = false;
        boolean hasBlackKing = false;
        boolean isEmpty = true;
        for (int piece : board) {
            if (piece == 6) {
                hasWhiteKing = true;
                if (hasBlackKing && isEmpty) break;
            } else if (piece == -6) {
                hasBlackKing = true;
                if (hasWhiteKing && isEmpty) break;
            } else if (piece != 0) {
                isEmpty = false;
                if (hasBlackKing && hasWhiteKing) break;
            }
        }
        if (hasWhiteKing && !hasBlackKing) winner = 1;
        else if (!hasWhiteKing && hasBlackKing) winner = -1;
        else if (isEmpty) winner = 0;
        isWinner = !hasWhiteKing || !hasBlackKing || isEmpty;
        return !isWinner;
    }

    public int getWinner() {
        return winner;
    }

    private void addMovesForKing(int i) {
        int idxMod8 = i % 8;
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                int destination = i + j * 8 + k;
                if (idxMod8 + k == destination % 8 && 0 <= destination && destination < 64 &&
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
        int idxMod8 = i % 8;
        int idxDiv8 = i / 8;
        for (int j = 1; j < 8; j++) {
            int target = i + direction1 * j + direction2 * j * 8;
            if (!(0 <= target && target < 64 && target % 8 == idxMod8 + direction1 * j &&
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
        int idxMod8 = i % 8;
        int target;
        for (int j = -2; j <= 2; j += 4) {
            for (int k = -1; k <= 1; k += 2) {
                target = i + j * 8 + k;
                if (idxMod8 + k == target % 8 && 0 <= target && target < 64
                        && board[target] * color <= 0)
                    addMoveSlot(i, target);
                target = i + j + k * 8;
                if (idxMod8 + j == target % 8 && 0 <= target && target < 64
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
                addMoveSlot(-2, i, 2 * color); // Knight
                addMoveSlot(-2, i, 3 * color); // Bishop
                addMoveSlot(-2, i, 4 * color); // Rook
                addMoveSlot(-2, i, 5 * color); // Queen
            } else {
                addMoveSlot(i, forwardSquare); // normal move
                if ((whiteMove ? i / 8 == 6 : i / 8 == 1) && board[i - 16 * color] == 0) {
                    addMoveSlot(i, i - 16 * color); // double move
                }
            }
        }

        // Capture Left
        if ((forwardSquare - 1 + 8) % 8 != 7 && board[forwardSquare - 1] * color < 0) {
            if (isPromotion) {
                addMoveSlot(-4, i, forwardSquare - 1); // Knight
                addMoveSlot(-5, i, forwardSquare - 1); // Bishop
                addMoveSlot(-6, i, forwardSquare - 1); // Rook
                addMoveSlot(-7, i, forwardSquare - 1); // Queen
            } else {
                addMoveSlot(i, forwardSquare - 1);
            }
        }

        // Capture Right
        if ((forwardSquare + 1) % 8 != 0 && board[forwardSquare + 1] * color < 0) {
            if (isPromotion) {
                addMoveSlot(-4, i, forwardSquare + 1); // Knight
                addMoveSlot(-5, i, forwardSquare + 1); // Bishop
                addMoveSlot(-6, i, forwardSquare + 1); // Rook
                addMoveSlot(-7, i, forwardSquare + 1); // Queen
            } else {
                addMoveSlot(i, forwardSquare + 1);
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
