import static java.lang.Math.abs;

public class MutableGameState {

    private byte[] board;
    public boolean whiteQueen;
    public boolean whiteKing;
    public boolean blackQueen;
    public boolean blackKing;
    private byte enPassantIdx;
    private int halfMoves;
    private byte halfMoveClock;
    private boolean whiteMove;
    private byte color;
    private PositionHistory positionHistory;
    private byte[] moves;
    private byte[] newMoves;
    private int moveCount;
    private boolean movesGenerated;
    private boolean onlyCapturesGenerated;
    private boolean isWinner;
    private byte winner;
    private byte blackKnights;
    private byte whiteKnights;
    private byte blackBishops;
    private byte whiteBishops;
    private byte otherPieces;
    private long hash;
    private byte whiteKingSquare;
    private byte blackKingSquare;
    public ZobristHash zobrist;
    byte lastMutatingMoveIdx;
    byte lastMutatingPieceTaken;
    private boolean canEnPassant;
    private int evaluation;
    private int curEval;

    private MutableGameState init(byte[] board, boolean whiteQueen, boolean whiteKing,
                                  boolean blackQueen, boolean blackKing, byte enPassantIdx,
                                  int halfMoves, byte halfMoveClock, boolean whiteMove,
                                  PositionHistory positionHistory, boolean isWinner, byte winner,
                                  byte blackKnights, byte whiteKnights, byte blackBishops,
                                  byte whiteBishops, byte otherPieces, byte whiteKingSquare,
                                  byte blackKingSquare, ZobristHash zobrist, boolean canEnPassant,
                                  long hash, int evaluation) {
        this.board = board;
        this.whiteQueen = whiteQueen;
        this.whiteKing = whiteKing;
        this.blackQueen = blackQueen;
        this.blackKing = blackKing;
        this.enPassantIdx = enPassantIdx;
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
        this.hash = hash;
        this.positionHistory = positionHistory == null ?
                this.positionHistory == null ? new PositionHistory(hash) :
                        this.positionHistory.init(hash) : positionHistory;
        this.canEnPassant = canEnPassant;
        this.moveCount = 0;
        this.movesGenerated = false;
        this.onlyCapturesGenerated = false;
        this.lastMutatingMoveIdx = -1;
        this.lastMutatingPieceTaken = -1;
        this.evaluation = evaluation;
        this.curEval = evaluation;
        return this;
    }

    MutableGameState(byte[] board, boolean whiteQueen, boolean whiteKing, boolean blackQueen,
                     boolean blackKing, byte enPassantIdx, int halfMoves, byte halfMoveClock,
                     boolean whiteMove, PositionHistory positionHistory, boolean isWinner,
                     byte winner, byte blackKnights, byte whiteKnights, byte blackBishops,
                     byte whiteBishops, byte otherPieces, byte whiteKingSquare,
                     byte blackKingSquare, ZobristHash zobrist, boolean canEnPassant, long hash,
                     int evaluation) {
        init(board, whiteQueen, whiteKing, blackQueen, blackKing, enPassantIdx, halfMoves,
                halfMoveClock, whiteMove, positionHistory, isWinner, winner, blackKnights,
                whiteKnights, blackBishops, whiteBishops, otherPieces, whiteKingSquare,
                blackKingSquare, zobrist, canEnPassant, hash, evaluation);
    }

    public void computeMoves() {
        if (movesGenerated) return;
        computeMovesPseudoLegal();
        byte pieceTaken;
        boolean illegal;
        if (newMoves == null) newMoves = new byte[654];
        int newMoveCount = 0;
        int idx;
        byte currKingIdx = getKingIdx();
        byte kingIdx, move_0, move_1, move_2;
        boolean kingMoved, override;
        if (currKingIdx == -1) return;
        boolean inCheck = inCheck();
        boolean inCheckByNonSlidingPiece = inCheckByNonSlidingPiece(currKingIdx, color);
        for (byte moveIdx = 0; moveIdx < moveCount; moveIdx++) {
            idx = moveIdx * 3;
            move_0 = moves[idx];
            move_1 = moves[idx + 1];
            move_2 = moves[idx + 2];
            pieceTaken = makeMoveOnlyBoard(move_0, move_1, move_2);
            illegal = false;
            if (move_0 == -1) {
                byte throughIdx = (byte) (currKingIdx + move_2);
                kingIdx = (byte) (throughIdx + move_2);
                if (isAttackingSliding(kingIdx, color) || isAttackingSliding(currKingIdx, color)
                        || isAttackingSliding(throughIdx, color)) illegal = true;
                else if (isAttackedByPawn(kingIdx, color) || isAttackedByPawn(currKingIdx, color)
                        || isAttackedByPawn(throughIdx, color)) illegal = true;
                else if (isAttackedByKing(kingIdx, color) || isAttackedByKing(currKingIdx, color)
                        || isAttackedByKing(throughIdx, color)) illegal = true;
                else if (isAttackedByKnight(kingIdx, color) || isAttackedByKnight(currKingIdx, color)
                        || isAttackedByKnight(throughIdx, color)) illegal = true;
            } else {
                kingMoved = move_0 >= 0 && abs(board[move_1]) == 6;
                kingIdx = kingMoved ? move_1 : currKingIdx;
                override = kingMoved || inCheck || move_0 < 0;
                if (isAttackingSliding(kingIdx, color, move_0, move_1, override)) illegal = true;
                else if (kingMoved || inCheckByNonSlidingPiece) {
                    if (isAttackedByPawn(kingIdx, color)) illegal = true;
                    else if (isAttackedByKnight(kingIdx, color)) illegal = true;
                    else if (isAttackedByKing(kingIdx, color)) illegal = true;
                }
            }
            undoMoveOnlyBoard(move_0, move_1, move_2, pieceTaken);
            if (!illegal) {
                newMoves[newMoveCount * 3] = move_0;
                newMoves[newMoveCount * 3 + 1] = move_1;
                newMoves[newMoveCount * 3 + 2] = move_2;
                newMoveCount++;
            }
        }
        byte[] tmp = moves;
        moves = newMoves;
        newMoves = tmp;
        movesGenerated = true;
        onlyCapturesGenerated = false;
        moveCount = newMoveCount;
    }

    public void computeMovesOnlyCaptures() {
        if (onlyCapturesGenerated) return;
        computeMovesPseudoLegalOnlyCaptures();
        byte pieceTaken;
        boolean illegal;
        if (newMoves == null) newMoves = new byte[654];
        int newMoveCount = 0;
        int idx;
        byte currKingIdx = getKingIdx();
        byte kingIdx, move_0, move_1, move_2;
        boolean kingMoved, override;
        if (currKingIdx == -1) return;
        boolean inCheck = inCheck();
        boolean inCheckByNonSlidingPiece = inCheckByNonSlidingPiece(currKingIdx, color);
        for (byte moveIdx = 0; moveIdx < moveCount; moveIdx++) {
            idx = moveIdx * 3;
            move_0 = moves[idx];
            move_1 = moves[idx + 1];
            move_2 = moves[idx + 2];
            pieceTaken = makeMoveOnlyBoard(move_0, move_1, move_2);
            illegal = false;
            kingMoved = move_0 >= 0 && abs(board[move_1]) == 6;
            kingIdx = kingMoved ? move_1 : currKingIdx;
            override = kingMoved || inCheck || move_0 < 0;
            if (isAttackingSliding(kingIdx, color, move_0, move_1, override)) illegal = true;
            else if (kingMoved || inCheckByNonSlidingPiece) {
                if (isAttackedByPawn(kingIdx, color)) illegal = true;
                else if (isAttackedByKnight(kingIdx, color)) illegal = true;
                else if (isAttackedByKing(kingIdx, color)) illegal = true;
            }
            undoMoveOnlyBoard(move_0, move_1, move_2, pieceTaken);
            if (!illegal) {
                newMoves[newMoveCount * 3] = move_0;
                newMoves[newMoveCount * 3 + 1] = move_1;
                newMoves[newMoveCount * 3 + 2] = move_2;
                newMoveCount++;
            }
        }
        byte[] tmp = moves;
        moves = newMoves;
        newMoves = tmp;
        movesGenerated = false;
        onlyCapturesGenerated = true;
        moveCount = newMoveCount;
    }

    private boolean isAttackingSliding(byte kingIdx, byte color) {
        return isAttackingSliding(kingIdx, color, (byte) 0, (byte) 0, true);
    }

    private boolean isAttackingSliding(byte kingIdx, byte color, byte move_0, byte move_1,
                                       boolean override) {
        byte piece, target;
        if (override || (move_0 - kingIdx) % 8 == 0 || (move_1 - kingIdx) % 8 == 0) {
            target = kingIdx;
            piece = 0;
            while (piece == 0 && target / 8 != 0) {
                target -= 8;
                piece = board[target];
                if (piece == -4 * color || piece == -5 * color) return true;
            }
            target = kingIdx;
            piece = 0;
            while (piece == 0 && target / 8 != 7) {
                target += 8;
                piece = board[target];
                if (piece == -4 * color || piece == -5 * color) return true;
            }
        }
        if (override || move_0 / 8 == kingIdx / 8 || move_1 / 8 == kingIdx / 8) {
            target = kingIdx;
            piece = 0;
            while (piece == 0 && target % 8 != 0) {
                target -= 1;
                piece = board[target];
                if (piece == -4 * color || piece == -5 * color) return true;
            }
            target = kingIdx;
            piece = 0;
            while (piece == 0 && target % 8 != 7) {
                target += 1;
                piece = board[target];
                if (piece == -4 * color || piece == -5 * color) return true;
            }
        }
        if (override || abs(move_0 - kingIdx) % 8 == abs(move_0 - kingIdx) / 8 ||
                abs(move_1 - kingIdx) % 8 == abs(move_1 - kingIdx) / 8) {
            target = kingIdx;
            piece = 0;
            while (piece == 0 && abs((target - 9) % 8 - target % 8) == 1) {
                target -= 9;
                if (target < 0 || target > 63) break;
                piece = board[target];
                if (piece == -3 * color || piece == -5 * color) return true;
            }
            target = kingIdx;
            piece = 0;
            while (piece == 0 && abs((target + 9) % 8 - target % 8) == 1) {
                target += 9;
                if (target < 0 || target > 63) break;
                piece = board[target];
                if (piece == -3 * color || piece == -5 * color) return true;
            }
        }
        if (override || 7 - abs(move_0 - kingIdx) % 8 == abs(move_0 - kingIdx) / 8 ||
                7 - abs(move_1 - kingIdx) % 8 == abs(move_1 - kingIdx) / 8) {
            target = kingIdx;
            piece = 0;
            while (piece == 0 && abs((target - 7) % 8 - target % 8) == 1) {
                target -= 7;
                if (target < 0 || target > 63) break;
                piece = board[target];
                if (piece == -3 * color || piece == -5 * color) return true;
            }
            target = kingIdx;
            piece = 0;
            while (piece == 0 && abs((target + 7) % 8 - target % 8) == 1) {
                target += 7;
                if (target < 0 || target > 63) break;
                piece = board[target];
                if (piece == -3 * color || piece == -5 * color) return true;
            }
        }
        return false;
    }

    private boolean isAttackedByKing(byte kingIdx, byte color) {
        byte destination;
        for (byte j = -1; j <= 1; j++) {
            for (byte k = -1; k <= 1; k++) {
                destination = (byte) (kingIdx + j * 8 + k);
                if ((kingIdx & 7) + k == (destination & 7) && 0 <= destination && destination <= 63
                        && board[destination] == -6 * color)
                    return true;
            }
        }
        return false;
    }

    private boolean isAttackedByKnight(byte kingIdx, byte color) {
        byte kingMod8 = (byte) (kingIdx & 7);
        byte target;
        for (byte j = -2; j <= 2; j += 4) {
            for (byte k = -1; k <= 1; k += 2) {
                target = (byte) (kingIdx + j * 8 + k);
                if (kingMod8 + k == (target & 7) && 0 <= target && target <= 63 &&
                        board[target] == -2 * color) return true;
                target = (byte) (kingIdx + j + k * 8);
                if (kingMod8 + j == (target & 7) && 0 <= target && target <= 63 &&
                        board[target] == -2 * color) return true;
            }
        }
        return false;
    }

    private boolean isAttackedByPawn(byte kingIdx, byte color) {
        byte forwardSquare = (byte) (kingIdx - 8 * color);
        if (forwardSquare < 0 || forwardSquare > 63) return false;

        return (((forwardSquare - 1) & 7) != 7) && board[forwardSquare - 1] == -color ||
                (((forwardSquare + 1) & 7) != 0) && board[forwardSquare + 1] == -color;
    }

    private byte getKingIdx() {
        return whiteMove ? whiteKingSquare : blackKingSquare;
    }

    private boolean inCheckByNonSlidingPiece(byte kingIdx, byte color) {
        if (isAttackedByPawn(kingIdx, color)) return true;
        else if (isAttackedByKnight(kingIdx, color)) return true;
        else return isAttackedByKing(kingIdx, color);
    }

    public boolean inCheck() {
        byte kingIdx = getKingIdx();
        if (isAttackingSliding(kingIdx, color)) return true;
        else if (isAttackedByPawn(kingIdx, color)) return true;
        else if (isAttackedByKnight(kingIdx, color)) return true;
        else return isAttackedByKing(kingIdx, color);
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

    public void computeMovesPseudoLegalOnlyCaptures() {
        if (moves == null) moves = new byte[654]; // 218 * 3
        moveCount = 0;
        byte pieceType;
        for (byte i = 0; i < 64; i++) {
            pieceType = (byte) (board[i] * color);
            if (pieceType <= 0) continue;
            switch (pieceType) {
                case 1:
                    addMovesForPawnOnlyCaptures(i);
                    break;
                case 2:
                    addMovesForKnightOnlyCaptures(i);
                    break;
                case 3:
                    addMovesForBishopOnlyCaptures(i);
                    break;
                case 4:
                    addMovesForRookOnlyCaptures(i);
                    break;
                case 5:
                    addMovesForQueenOnlyCaptures(i);
                    break;
                case 6:
                    addMovesForKingOnlyCaptures(i);
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

    public MutableGameState makeMove(int moveIdx) {
        int idx = moveIdx * 3;
        lastMutatingMoveIdx = (byte) moveIdx;
        return makeMove(moves[idx], moves[idx + 1], moves[idx + 2]);
    }

    public MutableGameState makeMove(byte move_0, byte move_1, byte move_2) {
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
        int score = evaluation;

        // Castle
        if (move_0 == -1) {
            newBoard[move_1 + move_2 * 2] = (byte) (6 * color);
            newBoard[move_1 + move_2] = (byte) (4 * color);
            newBoard[move_1] = 0;
            newBoard[move_1 + (move_2 == 1 ? 3 : -4)] = 0;
            score -= PieceSquareTables.getPieceSquareValue(4 * color,
                    move_1 + (move_2 == 1 ? 3 : -4));
            score -= PieceSquareTables.getPieceSquareValue(6 * color, move_1);
            score += PieceSquareTables.getPieceSquareValue(4 * color, move_1 + move_2);
            score += PieceSquareTables.getPieceSquareValue(6 * color, move_1 + move_2 * 2);
            if (whiteMove) {
                whiteQueen = false;
                whiteKing = false;
            } else {
                blackQueen = false;
                blackKing = false;
            }
            lastMutatingPieceTaken = 0;
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    (byte) -1, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove, null, false,
                    (byte) 0, blackKnights, whiteKnights, blackBishops, whiteBishops, otherPieces,
                    whiteMove ? (byte) (move_1 + move_2 * 2) : whiteKingSquare, !whiteMove ?
                    (byte) (move_1 + move_2 * 2) : blackKingSquare, zobrist, false, zobrist.hash(
                    newBoard, whiteQueen, whiteKing, blackQueen, blackKing, !whiteMove), score);
        }

        // Promotion
        if (move_0 == -2) {
            newBoard[move_1 - 8 * color] = move_2;
            newBoard[move_1] = 0;
            score -= PieceSquareTables.getPieceSquareValue(color, move_1);
            score += PieceSquareTables.getPieceSquareValue(move_2, move_1 - 8 * color);
            otherPieces--;
            if (move_2 == 2) whiteKnights++;
            else if (move_2 == 3) whiteBishops++;
            else if (move_2 == -2) blackKnights++;
            else if (move_2 == -3) blackBishops++;
            else otherPieces++;
            lastMutatingPieceTaken = 0;
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    (byte) -1, halfMoves + 1, (byte) 0, !whiteMove, null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops, whiteBishops, otherPieces,
                    whiteKingSquare, blackKingSquare, zobrist, false, zobrist.hash(newBoard,
                    whiteQueen, whiteKing, blackQueen, blackKing, !whiteMove), score);
        }

        // En Passant
        if (move_0 == -3) {
            newBoard[move_1 - color * 8 + move_2] = color;
            newBoard[move_1 + move_2] = 0;
            newBoard[move_1] = 0;
            score -= PieceSquareTables.getPieceSquareValue(color, move_1);
            score -= PieceSquareTables.getPieceSquareValue(-color, move_1 + move_2);
            score += PieceSquareTables.getPieceSquareValue(color, move_1 - color * 8 + move_2);
            lastMutatingPieceTaken = (byte) -color;
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    (byte) -1, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, (byte) (otherPieces - 1), whiteKingSquare, blackKingSquare,
                    zobrist, false, zobrist.hash(newBoard, whiteQueen, whiteKing, blackQueen,
                    blackKing, !whiteMove), score);
        }

        // Promotion Taking
        if (move_0 <= -4) {
            lastMutatingPieceTaken = board[move_2];
            newBoard[move_2] = (byte) ((-2 - move_0) * color);
            newBoard[move_1] = 0;
            score -= PieceSquareTables.getPieceSquareValue(color, move_1);
            score -= PieceSquareTables.getPieceSquareValue(lastMutatingPieceTaken, move_2);
            score += PieceSquareTables.getPieceSquareValue((byte) ((-2 - move_0) * color), move_2);
            otherPieces--;
            if (board[move_2] == 2) whiteKnights--;
            else if (board[move_2] == 3) whiteBishops--;
            else if (board[move_2] == -2) blackKnights--;
            else if (board[move_2] == -3) blackBishops--;
            else otherPieces--;
            if (move_0 == -4) {
                if (color == 1) whiteKnights++;
                else blackKnights++;
            } else if (move_0 == -5) {
                if (color == 1) whiteBishops++;
                else blackBishops++;
            } else otherPieces++;
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    (byte) -1, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove, null, false,
                    (byte) 0, blackKnights, whiteKnights, blackBishops, whiteBishops,
                    (byte) (otherPieces - 1), whiteKingSquare, blackKingSquare, zobrist, false,
                    zobrist.hash(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                            !whiteMove), score);
        }

        byte piece = newBoard[move_0];
        byte captured = newBoard[move_1];
        lastMutatingPieceTaken = captured;
        newBoard[move_0] = 0;
        newBoard[move_1] = piece;
        if (piece == -6) {
            blackQueen = false;
            blackKing = false;
        } else if (piece == 6) {
            whiteQueen = false;
            whiteKing = false;
        } else if (piece == -4) {
            if (move_0 == 0) blackQueen = false;
            else if (move_0 == 7) blackKing = false;
        } else if (piece == 4) {
            if (move_0 == 56) whiteQueen = false;
            else if (move_0 == 63) whiteKing = false;
        }

        long hash;
        hash = this.hash;
        hash ^= zobrist.hash(move_0, piece);
        hash ^= zobrist.hash(move_1, piece);
        hash ^= zobrist.hash(this.whiteQueen ^ whiteQueen,
                this.whiteKing ^ whiteKing,
                this.blackQueen ^ blackQueen,
                this.blackKing ^ blackKing, true);
        score -= PieceSquareTables.getPieceSquareValue(piece, move_0);
        score += PieceSquareTables.getPieceSquareValue(piece, move_1);
        if (captured != 0) {
            hash ^= zobrist.hash(move_1, captured);
            score -= PieceSquareTables.getPieceSquareValue(captured, move_1);
            if (captured == -4) {
                otherPieces--;
                if (move_1 == 0) blackQueen = false;
                else if (move_1 == 7) blackKing = false;
            } else if (captured == 4) {
                otherPieces--;
                if (move_1 == 56) whiteQueen = false;
                else if (move_1 == 63) whiteKing = false;
            } else if (captured == 2) whiteKnights--;
            else if (captured == 3) whiteBishops--;
            else if (captured == -2) blackKnights--;
            else if (captured == -3) blackBishops--;
            else otherPieces--;
        } else if ((piece == 1 || piece == -1) && abs(move_0 - move_1) == 16) {
            return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move_1, halfMoves + 1, (byte) 0, !whiteMove, null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops, whiteBishops, otherPieces,
                    whiteKingSquare, blackKingSquare, zobrist, true, hash, score);
        }

        byte halfMoveClock = piece == 1 || captured != 0 ? 0 : (byte) (this.halfMoveClock + 1);
        if (halfMoveClock > 0) {
            PositionHistory positionHistory = new PositionHistory(hash, this.positionHistory);
            return new MutableGameState(newBoard, whiteQueen, whiteKing,
                    blackQueen, blackKing, (byte) -1, halfMoves + 1, halfMoveClock,
                    !whiteMove, positionHistory, positionHistory.count >= 3, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces, piece == 6 ? move_1 : whiteKingSquare,
                    piece == -6 ? move_1 : blackKingSquare, zobrist, false, hash, score);
        }

        return new MutableGameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                (byte) -1, halfMoves + 1, (byte) 0, !whiteMove, null,
                false, (byte) 0, blackKnights, whiteKnights, blackBishops,
                whiteBishops, otherPieces, piece == 6 ? move_1 : whiteKingSquare,
                piece == -6 ? move_1 : blackKingSquare, zobrist, false, hash, score);
    }

    public MutableGameState loadMoveTo(MutableGameState child, int moveIdx) {
        int idx = moveIdx * 3;
        lastMutatingMoveIdx = (byte) moveIdx;
        return loadMoveTo(child, moves[idx], moves[idx + 1], moves[idx + 2]);
    }

    public MutableGameState loadMoveTo(MutableGameState child, byte move_0, byte move_1, byte move_2) {
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
        int score = this.evaluation;

        // Castle
        if (move_0 == -1) {
            newBoard[move_1 + move_2 * 2] = (byte) (6 * color);
            newBoard[move_1 + move_2] = (byte) (4 * color);
            newBoard[move_1] = 0;
            newBoard[move_1 + (move_2 == 1 ? 3 : -4)] = 0;
            score -= PieceSquareTables.getPieceSquareValue(4 * color,
                    move_1 + (move_2 == 1 ? 3 : -4));
            score -= PieceSquareTables.getPieceSquareValue(6 * color, move_1);
            score += PieceSquareTables.getPieceSquareValue(4 * color, move_1 + move_2);
            score += PieceSquareTables.getPieceSquareValue(6 * color, move_1 + move_2 * 2);
            if (whiteMove) {
                whiteQueen = false;
                whiteKing = false;
            } else {
                blackQueen = false;
                blackKing = false;
            }
            lastMutatingPieceTaken = 0;
            return child.init(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    (byte) -1, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove, null, false,
                    (byte) 0, blackKnights, whiteKnights, blackBishops, whiteBishops, otherPieces,
                    whiteMove ? (byte) (move_1 + move_2 * 2) : whiteKingSquare, !whiteMove ?
                            (byte) (move_1 + move_2 * 2) : blackKingSquare, zobrist, false, zobrist.hash(
                            newBoard, whiteQueen, whiteKing, blackQueen, blackKing, !whiteMove),
                    score);
        }

        // Promotion
        if (move_0 == -2) {
            newBoard[move_1 - 8 * color] = move_2;
            score -= PieceSquareTables.getPieceSquareValue(color, move_1);
            score += PieceSquareTables.getPieceSquareValue(move_2, move_1 - 8 * color);
            otherPieces--;
            if (move_2 == 2) whiteKnights++;
            else if (move_2 == 3) whiteBishops++;
            else if (move_2 == -2) blackKnights++;
            else if (move_2 == -3) blackBishops++;
            else otherPieces++;
            newBoard[move_1] = 0;
            lastMutatingPieceTaken = 0;
            return child.init(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    (byte) -1, halfMoves + 1, (byte) 0, !whiteMove, null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops, whiteBishops, otherPieces,
                    whiteKingSquare, blackKingSquare, zobrist, false, zobrist.hash(newBoard,
                            whiteQueen, whiteKing, blackQueen, blackKing, !whiteMove), score);
        }

        // En Passant
        if (move_0 == -3) {
            newBoard[move_1 - color * 8 + move_2] = color;
            newBoard[move_1 + move_2] = 0;
            newBoard[move_1] = 0;
            score -= PieceSquareTables.getPieceSquareValue(color, move_1);
            score -= PieceSquareTables.getPieceSquareValue(-color, move_1 + move_2);
            score += PieceSquareTables.getPieceSquareValue(color, move_1 - color * 8 + move_2);
            lastMutatingPieceTaken = (byte) -color;
            return child.init(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    (byte) -1, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, (byte) (otherPieces - 1), whiteKingSquare, blackKingSquare,
                    zobrist, false, zobrist.hash(newBoard, whiteQueen, whiteKing, blackQueen,
                            blackKing, !whiteMove), score);
        }

        // Promotion Taking
        if (move_0 <= -4) {
            lastMutatingPieceTaken = board[move_2];
            newBoard[move_2] = (byte) ((-2 - move_0) * color);
            newBoard[move_1] = 0;
            otherPieces--;
            score -= PieceSquareTables.getPieceSquareValue(color, move_1);
            score -= PieceSquareTables.getPieceSquareValue(lastMutatingPieceTaken, move_2);
            score += PieceSquareTables.getPieceSquareValue((byte) ((-2 - move_0) * color), move_2);
            if (board[move_2] == 2) whiteKnights--;
            else if (board[move_2] == 3) whiteBishops--;
            else if (board[move_2] == -2) blackKnights--;
            else if (board[move_2] == -3) blackBishops--;
            else otherPieces--;
            if (move_0 == -4) {
                if (color == 1) whiteKnights++;
                else blackKnights++;
            } else if (move_0 == -5) {
                if (color == 1) whiteBishops++;
                else blackBishops++;
            } else otherPieces++;
            return child.init(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    (byte) -1, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove, null, false,
                    (byte) 0, blackKnights, whiteKnights, blackBishops, whiteBishops,
                    (byte) (otherPieces - 1), whiteKingSquare, blackKingSquare, zobrist, false,
                    zobrist.hash(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                            !whiteMove), score);
        }

        byte piece = newBoard[move_0];
        byte captured = newBoard[move_1];
        lastMutatingPieceTaken = captured;
        newBoard[move_0] = 0;
        newBoard[move_1] = piece;
        if (piece == -6) {
            blackQueen = false;
            blackKing = false;
        } else if (piece == 6) {
            whiteQueen = false;
            whiteKing = false;
        } else if (piece == -4) {
            if (move_0 == 0) blackQueen = false;
            else if (move_0 == 7) blackKing = false;
        } else if (piece == 4) {
            if (move_0 == 56) whiteQueen = false;
            else if (move_0 == 63) whiteKing = false;
        }

        long hash;
        hash = this.hash;
        hash ^= zobrist.hash(move_0, piece);
        hash ^= zobrist.hash(move_1, piece);
        hash ^= zobrist.hash(this.whiteQueen ^ whiteQueen,
                this.whiteKing ^ whiteKing,
                this.blackQueen ^ blackQueen,
                this.blackKing ^ blackKing, true);
        score -= PieceSquareTables.getPieceSquareValue(piece, move_0);
        score += PieceSquareTables.getPieceSquareValue(piece, move_1);
        if (captured != 0) {
            hash ^= zobrist.hash(move_1, captured);
            score -= PieceSquareTables.getPieceSquareValue(captured, move_1);
            if (captured == -4) {
                otherPieces--;
                if (move_1 == 0) blackQueen = false;
                else if (move_1 == 7) blackKing = false;
            } else if (captured == 4) {
                otherPieces--;
                if (move_1 == 56) whiteQueen = false;
                else if (move_1 == 63) whiteKing = false;
            } else if (captured == 2) whiteKnights--;
            else if (captured == 3) whiteBishops--;
            else if (captured == -2) blackKnights--;
            else if (captured == -3) blackBishops--;
            else otherPieces--;
        } else if ((piece == 1 || piece == -1) && abs(move_0 - move_1) == 16) {
            return child.init(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    move_1, halfMoves + 1, (byte) 0, !whiteMove, null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops, whiteBishops, otherPieces,
                    whiteKingSquare, blackKingSquare, zobrist, true, hash, score);
        }

        byte halfMoveClock = piece == 1 || captured != 0 ? 0 : (byte) (this.halfMoveClock + 1);
        if (halfMoveClock > 0) {
            PositionHistory positionHistory = child.positionHistory == null ?
                    new PositionHistory(hash, this.positionHistory) :
                    child.positionHistory.init(hash, this.positionHistory);
            return child.init(newBoard, whiteQueen, whiteKing,
                    blackQueen, blackKing, (byte) -1, halfMoves + 1, halfMoveClock,
                    !whiteMove, positionHistory, positionHistory.count >= 3, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces, piece == 6 ? move_1 : whiteKingSquare,
                    piece == -6 ? move_1 : blackKingSquare, zobrist, false, hash, score);
        }

        return child.init(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                (byte) -1, halfMoves + 1, (byte) 0, !whiteMove, null,
                false, (byte) 0, blackKnights, whiteKnights, blackBishops,
                whiteBishops, otherPieces, piece == 6 ? move_1 : whiteKingSquare,
                piece == -6 ? move_1 : blackKingSquare, zobrist, false, hash, score);
    }

    public void undoMove() {
        int idx = lastMutatingMoveIdx * 3;
        undoMoveOnlyBoard(moves[idx], moves[idx + 1], moves[idx + 2], lastMutatingPieceTaken);
    }

    public void makeMoveOnlyBoard(int moveIdx) {
        int idx = moveIdx * 3;
        lastMutatingMoveIdx = (byte) moveIdx;
        lastMutatingPieceTaken = makeMoveOnlyBoard(moves[idx], moves[idx + 1], moves[idx + 2]);
    }

    public void makeMoveOnlyBoardEval(int moveIdx) {
        int idx = moveIdx * 3;
        lastMutatingMoveIdx = (byte) moveIdx;
        lastMutatingPieceTaken = makeMoveOnlyBoardEval(moves[idx], moves[idx + 1], moves[idx + 2]);
    }

    public byte makeMoveOnlyBoard(byte move_0, byte move_1, byte move_2) {
        // Castle
        if (move_0 == -1) {
            board[move_1 + move_2 * 2] = (byte) (6 * color);
            board[move_1 + move_2] = (byte) (4 * color);
            board[move_1] = 0;
            board[move_1 + (move_2 == 1 ? 3 : -4)] = 0;
            return 0;
        }

        // Promotion
        if (move_0 == -2) {
            board[move_1 - 8 * color] = move_2;
            board[move_1] = 0;
            return 0;
        }

        // En Passant
        if (move_0 == -3) {
            board[move_1 - color * 8 + move_2] = color;
            board[move_1 + move_2] = 0;
            board[move_1] = 0;
            return (byte) -color;
        }

        // Promotion Taking
        if (move_0 <= -4) {
            byte pieceTaken = board[move_2];
            board[move_2] = (byte) (color * (-2 - move_0));
            board[move_1] = 0;
            return pieceTaken;
        }

        byte pieceTaken = board[move_1];
        board[move_1] = board[move_0];
        board[move_0] = 0;

        return pieceTaken;
    }

    public void undoMoveOnlyBoard(byte move_0, byte move_1, byte move_2, byte pieceTaken) {
        // Castle
        if (move_0 == -1) {
            board[move_1 + move_2 * 2] = 0;
            board[move_1 + move_2] = 0;
            board[move_1] = (byte) (6 * color);
            board[move_1 + (move_2 == 1 ? 3 : -4)] = (byte) (4 * color);
            return;
        }

        // Promotion
        if (move_0 == -2) {
            board[move_1 - 8 * color] = 0;
            board[move_1] = color;
            return;
        }

        // En Passant
        if (move_0 == -3) {
            board[move_1 - color * 8 + move_2] = 0;
            board[move_1 + move_2] = pieceTaken;
            board[move_1] = color;
            return;
        }

        // Promotion Taking
        if (move_0 <= -4) {
            board[move_2] = pieceTaken;
            board[move_1] = color;
            return;
        }

        board[move_0] = board[move_1];
        board[move_1] = pieceTaken;
    }

    public byte makeMoveOnlyBoardEval(byte move_0, byte move_1, byte move_2) {
        curEval = evaluation;
        // Castle
        if (move_0 == -1) {
            board[move_1 + move_2 * 2] = (byte) (6 * color);
            board[move_1 + move_2] = (byte) (4 * color);
            board[move_1] = 0;
            board[move_1 + (move_2 == 1 ? 3 : -4)] = 0;
            curEval -= PieceSquareTables.getPieceSquareValue(4 * color,
                    move_1 + (move_2 == 1 ? 3 : -4));
            curEval -= PieceSquareTables.getPieceSquareValue(6 * color, move_1);
            curEval += PieceSquareTables.getPieceSquareValue(4 * color, move_1 + move_2);
            curEval += PieceSquareTables.getPieceSquareValue(6 * color, move_1 + move_2 * 2);
            return 0;
        }

        // Promotion
        if (move_0 == -2) {
            board[move_1 - 8 * color] = move_2;
            board[move_1] = 0;
            curEval -= PieceSquareTables.getPieceSquareValue(color, move_1);
            curEval += PieceSquareTables.getPieceSquareValue(move_2, move_1 - 8 * color);
            return 0;
        }

        // En Passant
        if (move_0 == -3) {
            board[move_1 - color * 8 + move_2] = color;
            board[move_1 + move_2] = 0;
            board[move_1] = 0;
            curEval -= PieceSquareTables.getPieceSquareValue(color, move_1);
            curEval -= PieceSquareTables.getPieceSquareValue(-color, move_1 + move_2);
            curEval += PieceSquareTables.getPieceSquareValue(color, move_1 - color * 8 + move_2);
            return (byte) -color;
        }

        // Promotion Taking
        if (move_0 <= -4) {
            byte pieceTaken = board[move_2];
            board[move_2] = (byte) (color * (-2 - move_0));
            board[move_1] = 0;
            curEval -= PieceSquareTables.getPieceSquareValue(color, move_1);
            curEval -= PieceSquareTables.getPieceSquareValue(pieceTaken, move_2);
            curEval += PieceSquareTables.getPieceSquareValue((byte) ((-2 - move_0) * color), move_2);
            return pieceTaken;
        }

        byte pieceTaken = board[move_1];
        byte piece = board[move_0];
        board[move_1] = piece;
        board[move_0] = 0;

        curEval -= PieceSquareTables.getPieceSquareValue(piece, move_0);
        curEval += PieceSquareTables.getPieceSquareValue(piece, move_1);
        curEval -= PieceSquareTables.getPieceSquareValue(pieceTaken, move_1);

        return pieceTaken;
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

    public long getHash() {
        return hash;
    }

    public int getEvaluation() {
        return evaluation;
    }

    public int getCurEval() {
        return curEval;
    }

    private void addMovesForKing(byte i) {
        byte idxMod8 = (byte) (i & 7);
        for (byte j = -1; j <= 1; j++) {
            for (byte k = -1; k <= 1; k++) {
                byte destination = (byte) (i + j * 8 + k);
                if (idxMod8 + k == (destination & 7) && 0 <= destination && destination < 64 &&
                        board[destination] * color <= 0) {
                    addMoveSlot(i, destination);
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
        if (enPassantIdx != -1 && canEnPassant) {
            if ((enPassantIdx & 7) == (i & 7) - 1 && enPassantIdx == i - 1) { // Left
                addMoveSlot((byte) -3, i, (byte) -1);
            } else if ((enPassantIdx & 7) == (i & 7) + 1 && enPassantIdx == i + 1) { // Right
                addMoveSlot((byte) -3, i, (byte) 1);
            }
        }
    }

    private void addMovesForKingOnlyCaptures(byte i) {
        byte idxMod8 = (byte) (i & 7);
        for (byte j = -1; j <= 1; j++) {
            for (byte k = -1; k <= 1; k++) {
                byte destination = (byte) (i + j * 8 + k);
                if (idxMod8 + k == (destination & 7) && 0 <= destination && destination < 64 &&
                        board[destination] * color < 0) {
                    addMoveSlot(i, destination);
                }
            }
        }
    }

    private void addSlidingMovesOnlyCaptures(byte i, byte direction1, byte direction2) {
        byte idxMod8 = (byte) (i & 7);
        byte idxDiv8 = (byte) (i / 8);
        for (byte j = 1; j < 8; j++) {
            byte target = (byte) (i + direction1 * j + direction2 * j * 8);
            if (!(0 <= target && target < 64 && (target & 7) == idxMod8 + direction1 * j &&
                    target / 8 == idxDiv8 + direction2 * j)) break;
            byte targetPieceType = (byte) (board[target] * color);
            if (targetPieceType == 0) continue;
            if (targetPieceType < 0) addMoveSlot(i, target);
            break;
        }
    }

    private void addMovesForQueenOnlyCaptures(byte i) {
        addSlidingMovesOnlyCaptures(i, (byte) 1, (byte) 1);
        addSlidingMovesOnlyCaptures(i, (byte) 1, (byte) -1);
        addSlidingMovesOnlyCaptures(i, (byte) -1, (byte) 1);
        addSlidingMovesOnlyCaptures(i, (byte) -1, (byte) -1);
        addSlidingMovesOnlyCaptures(i, (byte) 1, (byte) 0);
        addSlidingMovesOnlyCaptures(i, (byte) -1, (byte) 0);
        addSlidingMovesOnlyCaptures(i, (byte) 0, (byte) 1);
        addSlidingMovesOnlyCaptures(i, (byte) 0, (byte) -1);
    }

    private void addMovesForRookOnlyCaptures(byte i) {
        addSlidingMovesOnlyCaptures(i, (byte) 1, (byte) 0);
        addSlidingMovesOnlyCaptures(i, (byte) -1, (byte) 0);
        addSlidingMovesOnlyCaptures(i, (byte) 0, (byte) 1);
        addSlidingMovesOnlyCaptures(i, (byte) 0, (byte) -1);
    }

    private void addMovesForBishopOnlyCaptures(byte i) {
        addSlidingMovesOnlyCaptures(i, (byte) 1, (byte) 1);
        addSlidingMovesOnlyCaptures(i, (byte) 1, (byte) -1);
        addSlidingMovesOnlyCaptures(i, (byte) -1, (byte) 1);
        addSlidingMovesOnlyCaptures(i, (byte) -1, (byte) -1);
    }

    private void addMovesForKnightOnlyCaptures(byte i) {
        byte idxMod8 = (byte) (i & 7);
        byte target;
        for (byte j = -2; j <= 2; j += 4) {
            for (byte k = -1; k <= 1; k += 2) {
                target = (byte) (i + j * 8 + k);
                if (idxMod8 + k == (target & 7) && 0 <= target && target < 64
                        && board[target] * color < 0) addMoveSlot(i, target);
                target = (byte) (i + j + k * 8);
                if (idxMod8 + j == (target & 7) && 0 <= target && target < 64
                        && board[target] * color < 0) addMoveSlot(i, target);
            }
        }
    }

    private void addMovesForPawnOnlyCaptures(byte i) {
        byte forwardSquare = (byte) (i - 8 * color);
        boolean isPromotion = whiteMove ? i / 8 == 1 : i / 8 == 6;

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
        if (enPassantIdx != -1 && canEnPassant) {
            if ((enPassantIdx & 7) == (i & 7) - 1 && enPassantIdx == i - 1) { // Left
                addMoveSlot((byte) -3, i, (byte) -1);
            } else if ((enPassantIdx & 7) == (i & 7) + 1 && enPassantIdx == i + 1) { // Right
                addMoveSlot((byte) -3, i, (byte) 1);
            }
        }
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
