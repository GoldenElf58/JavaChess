import java.util.HashMap;

import static java.lang.Math.abs;

public class MutableGameState {

    private final byte[] board;
    public final boolean whiteQueen;
    public final boolean whiteKing;
    public final boolean blackQueen;
    public final boolean blackKing;
    private final byte[] lastMove;
    private final int halfMoves;
    private final byte halfMoveClock;
    private final boolean whiteMove;
    private final byte color;
    private final PositionHistory positionHistory;
    private byte[] moves;
    private int moveCount = 0;
    private boolean movesGenerated = false;
    private boolean isWinner;
    private byte winner;
    private final byte blackKnights;
    private final byte whiteKnights;
    private final byte blackBishops;
    private final byte whiteBishops;
    private final byte otherPieces;
    private long hash;
    private boolean hashSaved;
    private final byte whiteKingSquare;
    private final byte blackKingSquare;
    private final HashMap<Integer, MutableGameState> nextStates = new HashMap<>();
    private final ZobristHash zobrist;
    byte[] lastMutatingMove;
    byte lastMutatingPieceTaken;
    private final boolean canEnPassant;

    MutableGameState(byte[] board, boolean whiteQueen, boolean whiteKing, boolean blackQueen,
                     boolean blackKing, byte[] lastMove, int halfMoves, byte halfMoveClock,
                     boolean whiteMove, PositionHistory positionHistory, boolean isWinner,
                     byte winner, byte blackKnights, byte whiteKnights, byte blackBishops,
                     byte whiteBishops, byte otherPieces, byte whiteKingSquare, byte blackKingSquare,
                     ZobristHash zobrist, boolean canEnPassant) {
        this.board = board;
        this.whiteQueen = whiteQueen;
        this.whiteKing = whiteKing;
        this.blackQueen = blackQueen;
        this.blackKing = blackKing;
        this.lastMove = lastMove;
        this.halfMoves = halfMoves;
        this.halfMoveClock = halfMoveClock;
        this.whiteMove = whiteMove;
        this.color = (byte) (whiteMove ? 1 : -1);
        this.isWinner = isWinner;
        this.winner = winner;
        this.blackKnights = blackKnights;
        this.whiteKnights = whiteKnights;
        this.blackBishops = blackBishops;
        this.whiteBishops = whiteBishops;
        this.otherPieces = otherPieces;
        this.whiteKingSquare = whiteKingSquare;
        this.blackKingSquare = blackKingSquare;
        this.zobrist = zobrist;
        this.positionHistory = positionHistory == null ? new PositionHistory(getHash())
                : positionHistory;
        this.canEnPassant = canEnPassant;
    }

    public void computeMoves() {
        if (moveCount != 0) return;
        computeMovesPseudoLegal();
        byte[] board = this.board;
        byte pieceTaken;
        boolean illegal;
        byte[] newMoves = new byte[moveCount * 3];
        int newMoveCount = 0;
        byte[] move;
        byte currKingIdx = getKingIdx();
        byte kingIdx;
        boolean kingMoved;
        if (currKingIdx == -1) return;
        boolean inCheck = inCheckByNonSlidingPiece(currKingIdx);
        for (byte moveIdx = 0; moveIdx < moveCount; moveIdx++) {
            move = getMove(moveIdx);
            long hash = zobrist.hash(board);
//            System.out.println("before" + this);
            pieceTaken = makeMoveOnlyBoard(move);
//            System.out.println("after" + this);
            illegal = false;
            if (move[0] == -1) {
                byte throughIdx = (byte) (currKingIdx + move[2]);
                kingIdx = (byte) (throughIdx + move[2]);
                for (byte i = 0; i < 64; i++) {
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
                kingIdx = kingMoved ? move[1] : currKingIdx;
                for (byte i = 0; i < 64 && !illegal; i++) {
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
                }
            }
            undoMoveOnlyBoard(move, pieceTaken);
//            System.out.println("fixed" + this);
            if (hash != zobrist.hash(board)) {
                System.out.println("hash mismatch");
                throw new RuntimeException("hash mismatch");
            }
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

    private byte getKingIdx() {
        return whiteMove ? whiteKingSquare : blackKingSquare;
    }

    private boolean inCheckByNonSlidingPiece(byte kingIdx) {
        boolean inCheck;
        for (byte i = 0; i < 64; i++) {
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

    private boolean inCheck(byte kingIdx) {
        boolean inCheck;
        for (byte i = 0; i < 64; i++) {
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

    private boolean isPawnAttacking(byte pawnIdx, byte targetIdx) {
        byte forwardSquare = (byte) (pawnIdx + 8 * color);

        return (forwardSquare - 1 == targetIdx && ((forwardSquare - 1) & 7) != 7) ||
                (forwardSquare + 1 == targetIdx && ((forwardSquare + 1) & 7) != 0);
    }

    private boolean isKnightAttacking(byte knightIdx, byte targetIdx) {
        byte knightMod8 = (byte) (knightIdx & 7);
        for (byte j = -2; j <= 2; j += 4) {
            for (byte k = -1; k <= 1; k += 2) {
                byte target = (byte) (knightIdx + j * 8 + k);
                if (knightMod8 + k == (target & 7) && target == targetIdx) return true;
                target = (byte) (knightIdx + j + k * 8);
                if (knightMod8 + j == (target & 7) && target == targetIdx) return true;
            }
        }
        return false;
    }

    private boolean isDiagonalSlidingPieceAttacking(byte pieceIdx, byte col, byte row,
                                                    byte targetIdx, byte d1, byte d2) {
        for (byte j = 1; j < 8; j++) {
            col += d1;
            row += d2;
            if (col < 0 || col > 7 || row < 0 || row > 7) break;
            pieceIdx += (byte) (d1 + d2 * 8);
            if (pieceIdx == targetIdx) return true;
            if (pieceIdx < 0 || pieceIdx > 63 || board[pieceIdx] != 0) break;
        }
        return false;
    }

    private boolean isFileSlidingPieceAttacking(byte pieceIdx, byte row, byte targetIdx, byte dir) {
        for (byte j = 1; j < 8; j++) {
            row += dir;
            if (row < 0 || row > 7) break;
            pieceIdx += (byte) (dir * 8);
            if (pieceIdx == targetIdx) return true;
            if (board[pieceIdx] != 0) break;
        }
        return false;
    }

    private boolean isRankSlidingPieceAttacking(byte pieceIdx, byte col, byte targetIdx, byte dir) {
        for (byte j = 1; j < 8; j++) {
            col += dir;
            if (col < 0 || col > 7) break;
            pieceIdx += dir;
            if (pieceIdx == targetIdx) return true;
            if (board[pieceIdx] != 0) break;
        }
        return false;
    }

    private boolean isBishopAttacking(byte bishopIdx, byte targetIdx) {
        byte bishopMod8 = (byte) (bishopIdx & 7);
        byte bishopDiv8 = (byte) (bishopIdx / 8);
        return isDiagonalSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx,
                (byte) 1, (byte) 1)
                || isDiagonalSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx,
                (byte) 1, (byte) -1)
                || isDiagonalSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx,
                (byte) -1, (byte) 1)
                || isDiagonalSlidingPieceAttacking(bishopIdx, bishopMod8, bishopDiv8, targetIdx,
                (byte) -1, (byte) -1);
    }

    private boolean isRookAttacking(byte rookIdx, byte targetIdx) {
        byte rookMod8 = (byte) (rookIdx & 7);
        byte rookDiv8 = (byte) (rookIdx / 8);
        return isFileSlidingPieceAttacking(rookIdx, rookDiv8, targetIdx, (byte) 1)
                || isFileSlidingPieceAttacking(rookIdx, rookDiv8, targetIdx, (byte) -1)
                || isRankSlidingPieceAttacking(rookIdx, rookMod8, targetIdx, (byte) 1)
                || isRankSlidingPieceAttacking(rookIdx, rookMod8, targetIdx, (byte) -1);
    }

    private boolean isQueenAttacking(byte queenIdx, byte targetIdx) {
        byte queenMod8 = (byte) (queenIdx & 7);
        byte queenDiv8 = (byte) (queenIdx / 8);
        return isDiagonalSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, (byte) 1, (byte) 1)
                || isDiagonalSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx, (byte) 1, (byte) -1)
                || isDiagonalSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx,
                (byte) -1, (byte) 1)
                || isDiagonalSlidingPieceAttacking(queenIdx, queenMod8, queenDiv8, targetIdx,
                (byte) -1, (byte) -1)
                || isFileSlidingPieceAttacking(queenIdx, queenDiv8, targetIdx, (byte) 1)
                || isFileSlidingPieceAttacking(queenIdx, queenDiv8, targetIdx, (byte) -1)
                || isRankSlidingPieceAttacking(queenIdx, queenMod8, targetIdx, (byte) 1)
                || isRankSlidingPieceAttacking(queenIdx, queenMod8, targetIdx, (byte) -1);
    }

    private boolean isKingAttacking(byte kingIdx, byte targetIdx1, byte targetIdx2, byte targetIdx3) {
        for (byte j = -1; j <= 1; j++) {
            for (byte k = -1; k <= 1; k++) {
                byte destination = (byte) (kingIdx + j * 8 + k);
                if ((destination == targetIdx1 || destination == targetIdx2
                        || destination == targetIdx3) && (kingIdx & 7) + k == (destination & 7))
                    return true;
            }
        }
        return false;
    }

    private boolean isPawnAttacking(byte pawnIdx, byte targetIdx1, byte targetIdx2, byte targetIdx3) {
        byte forwardSquare = (byte) (pawnIdx + 8 * color);

        return ((forwardSquare - 1 == targetIdx1 || forwardSquare - 1 == targetIdx2 ||
                forwardSquare - 1 == targetIdx3) && ((forwardSquare - 1) & 7) != 7) ||
                ((forwardSquare + 1 == targetIdx1 || forwardSquare + 1 == targetIdx2 ||
                        forwardSquare + 1 == targetIdx3) && ((forwardSquare + 1) & 7) != 0);
    }

    private boolean isKnightAttacking(byte knightIdx, byte targetIdx1, byte targetIdx2,
                                      byte targetIdx3) {
        byte knightMod8 = (byte) (knightIdx & 7);
        for (byte j = -2; j <= 2; j += 4) {
            for (byte k = -1; k <= 1; k += 2) {
                byte target = (byte) (knightIdx + j * 8 + k);
                if (knightMod8 + k == (target & 7) && (target == targetIdx1
                        || target == targetIdx2 || target == targetIdx3)) return true;
                target = (byte) (knightIdx + j + k * 8);
                if (knightMod8 + j == (target & 7) && (target == targetIdx1
                        || target == targetIdx2 || target == targetIdx3)) return true;
            }
        }
        return false;
    }

    private boolean isBishopAttacking(byte bishopIdx, byte targetIdx1, byte targetIdx2, byte targetIdx3,
                                      byte[] board) {
        byte bishopMod8 = (byte) (bishopIdx & 7);
        byte bishopDiv8 = (byte) (bishopIdx / 8);
        for (byte d1 = -1; d1 <= 1; d1 += 2) {
            for (byte d2 = -1; d2 <= 1; d2 += 2) {
                for (byte j = 1; j < 8; j++) {
                    byte target = (byte) (bishopIdx + d1 * j + d2 * j * 8);
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

    private boolean isRookAttacking(byte rookIdx, byte targetIdx1, byte targetIdx2, byte targetIdx3,
                                    byte[] board) {
        byte rookMod8 = (byte) (rookIdx & 7);
        byte rookDiv8 = (byte) (rookIdx / 8);
        for (byte d1 = -1; d1 <= 1; d1 += 2) {
            for (byte j = 1; j < 8; j++) {
                byte target = (byte) (rookIdx + d1 * j);
                if (!((target & 7) == rookMod8 + d1 * j && target / 8 == rookDiv8))
                    break;
                if (target == targetIdx1 || target == targetIdx2 || target == targetIdx3)
                    return true;
                if (board[target] != 0) break;
            }
        }
        for (byte d2 = -1; d2 <= 1; d2 += 2) {
            for (byte j = 1; j < 8; j++) {
                byte target = (byte) (rookIdx + d2 * j * 8);
                if (!((target & 7) == rookMod8 && target / 8 == rookDiv8 + d2 * j))
                    break;
                if (target == targetIdx1 || target == targetIdx2 || target == targetIdx3)
                    return true;
                if (target < 0 || target > 63 || board[target] != 0) break;
            }
        }
        return false;
    }

    private boolean isQueenAttacking(byte queenIdx, byte targetIdx1, byte targetIdx2, byte targetIdx3,
                                     byte[] board) {
        byte queenMod8 = (byte) (queenIdx & 7);
        byte queenDiv8 = (byte) (queenIdx / 8);
        for (byte d1 = -1; d1 <= 1; d1++) {
            for (byte d2 = -1; d2 <= 1; d2++) {
                if (d1 == 0 && d2 == 0) continue;
                for (byte j = 1; j < 8; j++) {
                    byte target = (byte) (queenIdx + d1 * j + d2 * j * 8);
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

    private boolean isKingAttacking(byte kingIdx, byte targetIdx) {
        for (byte j = -1; j <= 1; j++) {
            for (byte k = -1; k <= 1; k++) {
                byte destination = (byte) (kingIdx + j * 8 + k);
                if (destination == targetIdx && (kingIdx & 7) + k == (destination & 7))
                    return true;
            }
        }
        return false;
    }

    public void computeMovesPseudoLegal() {
        if (moves == null) moves = new byte[654]; // 218 * 3
        moveCount = 0;
        byte pieceType;
        for (byte i = 0; i < 64; i++) {
            pieceType = (byte) (board[i] * color);
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

    private void addMoveSlot(byte a, byte b) {
        moves[moveCount * 3] = a;
        moves[moveCount * 3 + 1] = b;
        moveCount++;
    }

    private void addMoveSlot(byte a, byte b, byte c) {
        moves[moveCount * 3] = a;
        moves[moveCount * 3 + 1] = b;
        moves[moveCount * 3 + 2] = c;
        moveCount++;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public byte[] getMove(int moveIdx) {
        return new byte[]{moves[moveIdx * 3], moves[moveIdx * 3 + 1], moves[moveIdx * 3 + 2]};
    }

    public MutableGameState makeMove(int moveIdx) {
        return makeMove(getMove(moveIdx));
    }

    public MutableGameState makeMove(byte[] move) {
        byte[] newBoard = board;
        boolean whiteQueen = this.whiteQueen;
        boolean whiteKing = this.whiteKing;
        boolean blackQueen = this.blackQueen;
        boolean blackKing = this.blackKing;
        byte blackKnights = this.blackKnights;
        byte whiteKnights = this.whiteKnights;
        byte whiteBishops = this.whiteBishops;
        byte blackBishops = this.blackBishops;
        byte otherPieces = this.otherPieces;
        lastMutatingMove = move;

        // Castle
        if (move[0] == -1) {
            newBoard[move[1] + move[2] * 2] = (byte) (6 * color);
            newBoard[move[1] + move[2]] = (byte) (4 * color);
            newBoard[move[1]] = 0;
            newBoard[move[1] + (move[2] == 1 ? 3 : -4)] = 0;
            if (whiteMove) {
                whiteQueen = false;
                whiteKing = false;
            } else {
                blackQueen = false;
                blackKing = false;
            }
            lastMutatingPieceTaken = 0;
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces,
                    whiteMove ? (byte) (move[1] + move[2] * 2) : whiteKingSquare,
                    !whiteMove ? (byte) (move[1] + move[2] * 2) : blackKingSquare, zobrist, false);
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
            lastMutatingPieceTaken = 0;
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move, halfMoves + 1, (byte) 0, !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces, whiteKingSquare, blackKingSquare, zobrist, false);
        }

        // En Passant
        if (move[0] == -3) {
            newBoard[move[1] - color * 8 + move[2]] = color;
            newBoard[move[1] + move[2]] = 0;
            newBoard[move[1]] = 0;
            lastMutatingPieceTaken = (byte) -color;
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, (byte) (otherPieces - 1), whiteKingSquare, blackKingSquare,
                    zobrist, false);
        }

        // Promotion Taking
        if (move[0] <= -4) {
            lastMutatingPieceTaken = board[move[2]];
            newBoard[move[2]] = (byte) ((-2 - move[0]) * color);
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
            } else if (move[0] == -5) {
                if (color == 1) whiteBishops++;
                else blackBishops++;
            } else otherPieces++;
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, (byte) (otherPieces - 1), whiteKingSquare, blackKingSquare,
                    zobrist, false);
        }

        byte piece = newBoard[move[0]];
        byte captured = newBoard[move[1]];
        lastMutatingPieceTaken = captured;
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
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move, halfMoves + 1, (byte) 0,
                    !whiteMove, null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces, whiteKingSquare, blackKingSquare, zobrist, true);
        }

        byte halfMoveClock = piece == 1 || captured != 0 ? 0 : (byte) (this.halfMoveClock + 1);

        if (halfMoveClock > 0) {
            long hash = zobrist.hash(newBoard, whiteQueen, whiteKing, blackQueen, blackKing, !whiteMove);
            PositionHistory positionHistory =
                    new PositionHistory(hash, this.positionHistory);
            MutableGameState newGameState = new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen,
                    blackKing, move, halfMoves + 1, halfMoveClock,
                    !whiteMove, positionHistory, positionHistory.count >= 3, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces, piece == 6 ? move[1] : whiteKingSquare,
                    piece == -6 ? move[1] : blackKingSquare, zobrist, false);
            newGameState.setHash(hash);
            return newGameState;
        }

        return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                move, halfMoves + 1, (byte) 0, !whiteMove, null,
                false, (byte) 0, blackKnights, whiteKnights, blackBishops,
                whiteBishops, otherPieces, piece == 6 ? move[1] : whiteKingSquare,
                piece == -6 ? move[1] : blackKingSquare, zobrist, false);
    }

    public void undoMove() {
        undoMoveOnlyBoard(lastMutatingMove, lastMutatingPieceTaken);
    }

    public byte makeMoveOnlyBoard(byte[] move) {
        // Castle
        if (move[0] == -1) {
            board[move[1] + move[2] * 2] = (byte) (6 * color);
            board[move[1] + move[2]] = (byte) (4 * color);
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
            return (byte) -color;
        }

        // Promotion Taking
        if (move[0] <= -4) {
            byte pieceTaken = board[move[2]];
            board[move[2]] = (byte) (-2 - move[0]);
            board[move[1]] = 0;
            return pieceTaken;
        }

        byte pieceTaken = board[move[1]];
        board[move[1]] = board[move[0]];
        board[move[0]] = 0;

        return pieceTaken;
    }

    public void undoMoveOnlyBoard(byte[] move, byte pieceTaken) {
        // Castle
        if (move[0] == -1) {
            byte move_1 = move[1];
            board[move_1 + move[2] * 2] = 0;
            board[move_1 + move[2]] = 0;
            board[move_1] = (byte) (6 * color);
            board[move_1 + (move[2] == 1 ? 3 : -4)] = (byte) (4 * color);
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isInProgress() {
        if (isWinner) return false;
        if (halfMoveClock >= 100) {
            winner = 0;
            return false;
        }
        if (movesGenerated && moveCount == 0) {
            winner = inCheck() ? (byte) -color : 0;
            return false;
        }
        if (otherPieces == 0) {
            byte minorPieces = (byte) (whiteKnights + whiteBishops + blackKnights + blackBishops);
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

    public byte getWinner() {
        return winner;
    }

    public byte[] getBoard() {
        return board;
    }

    public boolean isWhiteMove() {
        return whiteMove;
    }

    private void addMovesForKing(byte i) {
        byte idxMod8 = (byte) (i & 7);
        for (byte j = -1; j <= 1; j++) {
            for (byte k = -1; k <= 1; k++) {
                byte destination = (byte) (i + j * 8 + k);
                if (idxMod8 + k == (destination & 7) && 0 <= destination && destination < 64 &&
                        board[destination] * color <= 0) {
                    addMoveSlot(i, destination, (byte) 6);
                }
            }
        }

        if ((whiteMove ? whiteKing : blackKing) &&
                board[i + 1] == 0 && board[i + 2] == 0 && board[i + 3] == 4 * color) {
            addMoveSlot((byte) -1, i, (byte) 1);
        }

        if (((whiteMove ? whiteQueen : blackQueen)) && board[i - 1] == 0 && board[i - 2] == 0 &&
                board[i - 3] == 0 && board[i - 4] == 4 * color) {
            addMoveSlot((byte) -1, i, (byte) -1);
        }
    }

    private void addSlidingMoves(byte i, byte direction1, byte direction2) {
        byte idxMod8 = (byte) (i & 7);
        byte idxDiv8 = (byte) (i / 8);
        for (byte j = 1; j < 8; j++) {
            byte target = (byte) (i + direction1 * j + direction2 * j * 8);
            if (!(0 <= target && target < 64 && (target & 7) == idxMod8 + direction1 * j &&
                    target / 8 == idxDiv8 + direction2 * j))
                break;
            byte targetPieceType = (byte) (board[target] * color);
            if (targetPieceType == 0) {
                addMoveSlot(i, target);
                continue;
            }
            if (targetPieceType < 0)
                addMoveSlot(i, target);
            break;
        }
    }

    private void addMovesForQueen(byte i) {
        addSlidingMoves(i, (byte) 1, (byte) 1);
        addSlidingMoves(i, (byte) 1, (byte) -1);
        addSlidingMoves(i, (byte) -1, (byte) 1);
        addSlidingMoves(i, (byte) -1, (byte) -1);
        addSlidingMoves(i, (byte) 1, (byte) 0);
        addSlidingMoves(i, (byte) -1, (byte) 0);
        addSlidingMoves(i, (byte) 0, (byte) 1);
        addSlidingMoves(i, (byte) 0, (byte) -1);
    }

    private void addMovesForRook(byte i) {
        addSlidingMoves(i, (byte) 1, (byte) 0);
        addSlidingMoves(i, (byte) -1, (byte) 0);
        addSlidingMoves(i, (byte) 0, (byte) 1);
        addSlidingMoves(i, (byte) 0, (byte) -1);
    }

    private void addMovesForBishop(byte i) {
        addSlidingMoves(i, (byte) 1, (byte) 1);
        addSlidingMoves(i, (byte) 1, (byte) -1);
        addSlidingMoves(i, (byte) -1, (byte) 1);
        addSlidingMoves(i, (byte) -1, (byte) -1);
    }

    private void addMovesForKnight(byte i) {
        byte idxMod8 = (byte) (i & 7);
        byte target;
        for (byte j = -2; j <= 2; j += 4) {
            for (byte k = -1; k <= 1; k += 2) {
                target = (byte) (i + j * 8 + k);
                if (idxMod8 + k == (target & 7) && 0 <= target && target < 64
                        && board[target] * color <= 0)
                    addMoveSlot(i, target);
                target = (byte) (i + j + k * 8);
                if (idxMod8 + j == (target & 7) && 0 <= target && target < 64
                        && board[target] * color <= 0)
                    addMoveSlot(i, target);
            }
        }
    }

    private void addMovesForPawn(byte i) {
        byte forwardSquare = (byte) (i - 8 * color);
        boolean isPromotion = whiteMove ? i / 8 == 1 : i / 8 == 6;

        if (board[forwardSquare] == 0) {
            if (isPromotion) {
                addMoveSlot((byte) -2, i, (byte) (5 * color)); // Queen
                addMoveSlot((byte) -2, i, (byte) (4 * color)); // Rook
                addMoveSlot((byte) -2, i, (byte) (3 * color)); // Bishop
                addMoveSlot((byte) -2, i, (byte) (2 * color)); // Knight
            } else {
                addMoveSlot(i, forwardSquare); // normal move
                if ((whiteMove ? i / 8 == 6 : i / 8 == 1) && board[i - 16 * color] == 0) {
                    addMoveSlot(i, (byte) (i - 16 * color)); // double move
                }
            }
        }

        // Capture Left
        if (((forwardSquare - 1) & 7) != 7 && board[forwardSquare - 1] * color < 0) {
            if (isPromotion) {
                addMoveSlot((byte) -7, i, (byte) (forwardSquare - 1)); // Queen
                addMoveSlot((byte) -6, i, (byte) (forwardSquare - 1)); // Rook
                addMoveSlot((byte) -5, i, (byte) (forwardSquare - 1)); // Bishop
                addMoveSlot((byte) -4, i, (byte) (forwardSquare - 1)); // Knight
            } else {
                addMoveSlot(i, (byte) (forwardSquare - 1));
            }
        }

        // Capture Right
        if (((forwardSquare + 1) & 7) != 0 && board[forwardSquare + 1] * color < 0) {
            if (isPromotion) {
                addMoveSlot((byte) -7, i, (byte) (forwardSquare + 1)); // Queen
                addMoveSlot((byte) -6, i, (byte) (forwardSquare + 1)); // Rook
                addMoveSlot((byte) -5, i, (byte) (forwardSquare + 1)); // Bishop
                addMoveSlot((byte) -4, i, (byte) (forwardSquare + 1)); // Knight
            } else {
                addMoveSlot(i, (byte) (forwardSquare + 1));
            }
        }

        // En Passant
        if (lastMove != null && canEnPassant) {
            if ((lastMove[1] & 7) == (i & 7) - 1 && lastMove[1] == i - 1) { // Left
                addMoveSlot((byte) -3, i, (byte) -1);
            } else if ((lastMove[1] & 7) == (i & 7) + 1 && lastMove[1] == i + 1) { // Right
                addMoveSlot((byte) -3, i, (byte) 1);
            }
        }
    }

    public void setHash(long hash) {
        this.hash = hash;
        hashSaved = true;
    }

    public long getHash() {
       if (hashSaved) return hash;
       hashSaved = true;
       return hash = zobrist.hash(this);
    }

    public void saveState(int move, MutableGameState state) {
        nextStates.put(move, state);
    }

    public MutableGameState getState(int move) {
        return nextStates.getOrDefault(move, null);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (byte i = 0; i < 8; i++) {
            for (byte j = 0; j < 8; j++) {
                result.append(board[i * 8 + j]).append((board[i * 8 + j] >= 0 ? "  " : " "));
            }
            result.append("\n");
        }
        return result.toString();
    }
}
