import static java.lang.Math.abs;

public class GameState {

    static private final byte[] startBoard = {
            -4, -2, -3, -5, -6, -3, -2, -4,
            -1, -1, -1, -1, -1, -1, -1, -1,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1, 1, 1, 1, 1,
            4, 2, 3, 5, 6, 3, 2, 4
    };
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
    private final ZobristHash zobrist;
    private static final byte[] bishopDirs = {9, -9, 7, -7};

    GameState() {
        zobrist = new ZobristHash();
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
        positionHistory = new PositionHistory(zobrist.hash(this));
        isWinner = false;
        winner = 0;
        blackKnights = 2;
        whiteKnights = 2;
        blackBishops = 2;
        whiteBishops = 2;
        otherPieces = 22;
        whiteKingSquare = 60;
        blackKingSquare = 4;
    }

    GameState(byte[] board, boolean whiteQueen, boolean whiteKing, boolean blackQueen,
              boolean blackKing, byte[] lastMove, int halfMoves, byte halfMoveClock,
              boolean whiteMove, PositionHistory positionHistory, boolean isWinner,
              byte winner, byte blackKnights, byte whiteKnights, byte blackBishops,
              byte whiteBishops, byte otherPieces, byte whiteKingSquare, byte blackKingSquare,
              ZobristHash zobrist) {
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
    }

    public void computeMoves() {
        if (moveCount != 0) return;
        computeMovesPseudoLegal();
        byte pieceTaken;
        boolean illegal;
        byte[] newMoves = new byte[moveCount * 3];
        int newMoveCount = 0;
        byte[] move;
        byte currKingIdx = getKingIdx();
        byte kingIdx;
        boolean kingMoved;
        if (currKingIdx == -1) return;
        boolean inCheckByNonSlidingPiece = inCheckByNonSlidingPiece(currKingIdx, color);
        boolean inCheck = inCheck();
        boolean override;
        byte move_0, move_1;
        for (byte moveIdx = 0; moveIdx < moveCount; moveIdx++) {
            move = getMove(moveIdx);
            pieceTaken = makeMoveOnlyBoard(move);
            illegal = false;
            if (move[0] == -1) {
                byte throughIdx = (byte) (currKingIdx + move[2]);
                kingIdx = (byte) (throughIdx + move[2]);
                if (isAttackingOrthagonally(kingIdx, color)
                        || isAttackingOrthagonally(currKingIdx, color)
                        || isAttackingOrthagonally(throughIdx, color)) illegal = true;
                else if (isAttackingDiagonally(kingIdx, color)
                        || isAttackingDiagonally(currKingIdx, color)
                        || isAttackingDiagonally(throughIdx, color)) illegal = true;
                else if (isAttackedByPawn(kingIdx, color) || isAttackedByPawn(currKingIdx, color)
                        || isAttackedByPawn(throughIdx, color)) illegal = true;
                else if (isAttackedByKing(kingIdx, color) || isAttackedByKing(currKingIdx, color)
                        || isAttackedByKing(throughIdx, color)) illegal = true;
                else if (isAttackedByKnight(kingIdx, color)
                        || isAttackedByKnight(currKingIdx, color)
                        || isAttackedByKnight(throughIdx, color)) illegal = true;
            } else {
                move_0 = move[0];
                move_1 = move[1];
                kingMoved = move_0 >= 0 && move[2] == 6;
                kingIdx = kingMoved ? move_1 : currKingIdx;
                override = kingMoved || inCheck || move_0 < 0;
                if (isAttackingSliding(kingIdx, color, move_0, move_1, override)) illegal = true;
                else if (kingMoved || inCheckByNonSlidingPiece) {
                    if (isAttackedByPawn(kingIdx, color)) illegal = true;
                    else if (isAttackedByKnight(kingIdx, color)) illegal = true;
                    else if (isAttackedByKing(kingIdx, color)) illegal = true;
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

    private boolean isAttackingOrthagonally(byte kingIdx, byte color) {
        byte piece, target;
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
        return false;
    }

    private boolean isAttackingDiagonally(byte kingIdx, byte color) {
        byte piece, target;
        for (byte dir : bishopDirs) {
            target = kingIdx;
            piece = 0;
            while (piece == 0 && abs((target + dir) % 8 - target % 8) == 1) {
                target += dir;
                if (target < 0 || target > 63) break;
                piece = board[target];
                if (piece == -3 * color || piece == -5 * color) return true;
            }
        }
        return false;
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
        if (isAttackingOrthagonally(kingIdx, color)) return true;
        else if (isAttackingDiagonally(kingIdx, color)) return true;
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

    public GameState makeMove(int moveIdx) {
        return makeMove(getMove(moveIdx));
    }

    public GameState makeMove(byte[] move) {
        byte[] newBoard = board.clone();
        boolean whiteQueen = this.whiteQueen;
        boolean whiteKing = this.whiteKing;
        boolean blackQueen = this.blackQueen;
        boolean blackKing = this.blackKing;
        byte blackKnights = this.blackKnights;
        byte whiteKnights = this.whiteKnights;
        byte whiteBishops = this.whiteBishops;
        byte blackBishops = this.blackBishops;
        byte otherPieces = this.otherPieces;

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
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces,
                    whiteMove ? (byte) (move[1] + move[2] * 2) : whiteKingSquare,
                    !whiteMove ? (byte) (move[1] + move[2] * 2) : blackKingSquare, zobrist);
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
                    null, halfMoves + 1, (byte) 0, !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces, whiteKingSquare, blackKingSquare, zobrist);
        }

        // En Passant
        if (move[0] == -3) {
            newBoard[move[1] - color * 8 + move[2]] = color;
            newBoard[move[1] + move[2]] = 0;
            newBoard[move[1]] = 0;
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, (byte) (otherPieces - 1), whiteKingSquare, blackKingSquare, zobrist);
        }

        // Promotion Taking
        if (move[0] <= -4) {
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
            return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                    null, halfMoves + 1, (byte) (halfMoveClock + 1), !whiteMove,
                    null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, (byte) (otherPieces - 1), whiteKingSquare, blackKingSquare, zobrist);
        }

        byte piece = newBoard[move[0]];
        byte captured = newBoard[move[1]];
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
                    move, halfMoves + 1, (byte) 0,
                    !whiteMove, null, false, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces, whiteKingSquare, blackKingSquare, zobrist);
        }

        byte halfMoveClock = piece == 1 || captured != 0 ? 0 : (byte) (this.halfMoveClock + 1);

        if (halfMoveClock > 0) {
            long hash = zobrist.hash(newBoard, whiteQueen, whiteKing, blackQueen, blackKing, !whiteMove);
            PositionHistory positionHistory =
                    new PositionHistory(hash, this.positionHistory);
            GameState newGameState = new GameState(newBoard, whiteQueen, whiteKing, blackQueen,
                    blackKing, null, halfMoves + 1, halfMoveClock,
                    !whiteMove, positionHistory, positionHistory.count >= 3, (byte) 0,
                    blackKnights, whiteKnights, blackBishops,
                    whiteBishops, otherPieces, piece == 6 ? move[1] : whiteKingSquare,
                    piece == -6 ? move[1] : blackKingSquare, zobrist);
            newGameState.setHash(hash);
            return newGameState;
        }

        return new GameState(newBoard, whiteQueen, whiteKing, blackQueen, blackKing,
                null, halfMoves + 1, (byte) 0, !whiteMove, null,
                false, (byte) 0, blackKnights, whiteKnights, blackBishops,
                whiteBishops, otherPieces, piece == 6 ? move[1] : whiteKingSquare,
                piece == -6 ? move[1] : blackKingSquare, zobrist);
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
            board[move[2]] = (byte) (color * (-2 - move[0]));
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

    public byte getColor() {
        return color;
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
        if (lastMove != null) {
            if ((lastMove[1] & 7) == (i & 7) - 1 && lastMove[1] == i - 1) { // Left
                addMoveSlot((byte) -3, i, (byte) -1);
            } else if ((lastMove[1] & 7) == (i & 7) + 1 && lastMove[1] == i + 1) { // Right
                addMoveSlot((byte) -3, i, (byte) 1);
            }
        }
    }

    public byte[] findMove(int from, int to) {
        if (to == from || from == -1) return null;
        for (int i = 0; i < moveCount; i++) {
            byte[] move = this.getMove(i);
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

    public void setHash(long hash) {
        this.hash = hash;
        hashSaved = true;
    }

    public long getHash() {
        if (hashSaved) return hash;
        hashSaved = true;
        return hash = zobrist.hash(this);
    }

    public MutableGameState asMutable() {
        int score = 0;
        for (byte i = 0; i < 64; i++) score += PieceSquareTables.getPieceSquareValue(board[i], i);
        return new MutableGameState(board.clone(), whiteQueen, whiteKing, blackQueen, blackKing,
                lastMove == null ? -1 : lastMove[1], halfMoves, halfMoveClock, whiteMove,
                positionHistory, isWinner, winner, blackKnights, whiteKnights, blackBishops,
                whiteBishops, otherPieces, whiteKingSquare, blackKingSquare, zobrist,
                lastMove != null, getHash(), score);
    }

    public String moveToString(int moveIdx) {
        return moveToString(getMove(moveIdx));
    }

    public String moveToString(byte[] move) {
        return String.format("%c%d %c%d", 'a' + (move[0] & 7), 8 - move[0] / 8,
                'a' + (move[1] & 7), 8 - move[1] / 8);
    }

    public String moveRepr(int moveIdx) {
        return moveRepr(getMove(moveIdx));
    }

    public String moveRepr(byte[] move) {
        return String.format("(%d, %d, %d)", move[0], move[1], move[2]);
    }

    public void printMoves() {
        for (int i = 0; i < moveCount; i++) {
            System.out.printf("%s | %s%n", moveRepr(i), moveToString(i));
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
