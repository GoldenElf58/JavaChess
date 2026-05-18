package eval;

import game.GameState;
import game.MutableGameState;
import utils.Watch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

public class BotV4UsePartialState implements Bot {

    private int score;
    private final HashMap<Long, TTEntry> moveCache = new HashMap<>();
    private final boolean log;
    private boolean print;
    private final TTEntry emptyTT = new TTEntry();
    private MutableGameState[][] pools = new MutableGameState[10][218];
    private volatile boolean stopSearch = false;
    private int lastDepth;

    private static class TTEntry {
        int score;
        int depthRemaining;
    }

    public int getLastEval() {
        return score;
    }

    private static class StopSearchException extends RuntimeException {
        public StopSearchException() {
            super("StopSearchException");
        }
    }

    public BotV4UsePartialState(boolean log, boolean print) {
        this.log = log;
        this.print = print;
    }

    public void setPrinting(boolean print) {
        this.print = print;
    }

    public void clearCache() {
        moveCache.clear();
    }

    private int evaluate(MutableGameState state, boolean partial) {
        return partial ? state.getCurEval() : state.getEvaluation();
    }

    public int getMove(GameState state, double allottedTime) {
        return iterativeDeepening(state, allottedTime);
    }

    public int getLastDepth() {
        return lastDepth;
    }

    private int iterativeDeepening(GameState state, double allottedTime) {
        Watch watch = new Watch();
        watch.start();
        clearCache();
        stopSearch = false;
        final int[] depth = {1};
        final int[] move = {-1};
        Thread thread = new Thread(() -> {
        });
        score = 0;
        pools = new MutableGameState[10][218];
        while (watch.getElapsedTimeMillis() / 1000d < allottedTime) {
            if (!thread.isAlive()) {
                if (score == Integer.MAX_VALUE / 2 || score == Integer.MIN_VALUE / 2) break;
                thread = new Thread(() -> {
                    try {
                        pools = new MutableGameState[10][218];
                        move[0] = minimaxMove(state.asMutable(), depth[0], state.isWhiteMove(),
                                move[0]);
                        depth[0]++;
                    } catch (StopSearchException _) {
                    }
                });
                thread.start();
            }
        }
        stopSearch = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            thread.interrupt();
        }
        if (log) {
            if (!new File("depths.txt").exists()) {
                IO.println("Creating file depths.txt");
                try {
                    if (!new File("depths.txt").createNewFile())
                        IO.println("Failed to create file depths.txt");
                } catch (IOException e) {
                    IO.println("Failed to create file depths.txt");
                }
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter("depths.txt", true))) {
                writer.println(depth[0]);
            } catch (IOException e) {
                IO.println("Failed to append to file depths.txt");
            }
        }
        if (print) {
            IO.println("Depth: " + depth[0]);
            IO.println("Score: " + score);
            IO.println("Time: " + watch.getElapsedTimeMillis() / 1000d + "s");
        }
        clearCache();
        lastDepth = depth[0];
        return move[0] == -1 ? 0 : move[0];
    }

    public int iterativeDeepening(GameState state, int maxDepth) {
        Watch watch = new Watch();
        watch.start();
        stopSearch = false;
        clearCache();
        int depth = 1;
        int move = -1;
        score = 0;
        while (depth <= maxDepth) {
            if (score == Integer.MAX_VALUE / 2 || score == Integer.MIN_VALUE / 2) break;
            move = minimaxMove(state.asMutable(), depth, state.isWhiteMove(), move);
            depth++;
        }
        if (log) {
            if (!new File("depths.txt").exists()) {
                IO.println("Creating file depths.txt");
                try {
                    if (!new File("depths.txt").createNewFile())
                        IO.println("Failed to create file depths.txt");
                } catch (IOException e) {
                    IO.println("Failed to create file depths.txt");
                }
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter("depths.txt", true))) {
                writer.println(depth);
            } catch (IOException e) {
                IO.println("Failed to append to file depths.txt");
            }
        }
        if (print) {
            IO.println("Depth: " + depth);
            IO.println("Score: " + score);
        }
        clearCache();
        lastDepth = depth;
        return move == -1 ? 0 : move;
    }

    private int minimaxMove(MutableGameState state, int depth, boolean isMaximizing,
                            int bestMoveIdx) {
        assert depth > 0;
        state.computeMoves();
        if (!state.isInProgress()) return -1;
        int bestScore = isMaximizing ? Integer.MIN_VALUE / 2 : Integer.MAX_VALUE / 2;
        int alpha = Integer.MIN_VALUE / 2;
        int beta = Integer.MAX_VALUE / 2;

        final Integer[] moveSearchOrder;
        final MutableGameState[] nextStates;
        final boolean sortMoves = depth > 2;
        if (sortMoves) {
            moveSearchOrder = new Integer[state.getMoveCount()];
            nextStates = new MutableGameState[state.getMoveCount()];
            for (int i = 0; i < state.getMoveCount(); i++) {
                moveSearchOrder[i] = i;
                nextStates[i] = state.makeMove(i);
                state.undoMove();
            }
            int finalBestMoveIdx = bestMoveIdx;
            Arrays.sort(moveSearchOrder, (m1, m2) -> m1 == finalBestMoveIdx ? -1 :
             m2 == finalBestMoveIdx ? 1 : (isMaximizing ? -1 : 1) *
                    (moveCache.getOrDefault(nextStates[m1].getHash(), emptyTT).score
                            - moveCache.getOrDefault(nextStates[m2].getHash(), emptyTT).score));
        } else {
            nextStates = null;
            moveSearchOrder = null;
        }

        MutableGameState nextState;
        if (depth + 1 >= pools.length)
            pools = new MutableGameState[Math.max(depth, pools.length * 2) + 1][218];
        for (int i = 0; i < state.getMoveCount(); i++) {
            if (sortMoves) state.makeMoveOnlyBoard(moveSearchOrder[i]);
            nextState = sortMoves ? nextStates[moveSearchOrder[i]] : state.makeMove(i);
            int score = minimaxScore(nextState, 1, depth, !isMaximizing, alpha, beta);
            state.undoMove();
            if (stopSearch) {
                if (i > 0) this.score = bestScore;
                return bestMoveIdx;
            }
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMoveIdx = sortMoves ? moveSearchOrder[i] : i;
                if (isMaximizing) alpha = score;
                else beta = score;
            }
        }
        this.score = bestScore;
        return bestMoveIdx;
    }

    private int minimaxScore(MutableGameState state, int depth, int maxDepth, boolean isMaximizing,
                             int alpha, int beta) {
        if (stopSearch) return 0;
        if (depth == maxDepth) return evaluate(state, false);
        long hashKey = state.getHash();
        TTEntry thisEntry;
        if (moveCache.containsKey(hashKey)) {
            TTEntry entry = moveCache.get(hashKey);
            if (entry.depthRemaining >= maxDepth - depth) return entry.score;
            thisEntry = entry;
        } else thisEntry = new TTEntry();
        state.computeMoves();
        if (!state.isInProgress()) return state.getWinner() * Integer.MAX_VALUE / 2;

        final Integer[] moveSearchOrder;
        final MutableGameState[] nextStates;
        final boolean sortMoves = maxDepth - depth > 2;
        if (maxDepth - depth > 3) {
            moveSearchOrder = new Integer[state.getMoveCount()];
            nextStates = new MutableGameState[state.getMoveCount()];
            for (int i = 0; i < state.getMoveCount(); i++) {
                moveSearchOrder[i] = i;
                if (pools[depth][i] != null) nextStates[i] = state.loadMoveTo(pools[depth][i], i);
                else nextStates[i] = pools[depth][i] = state.makeMove(i);
                state.undoMove();
            }
            Arrays.sort(moveSearchOrder, (m1, m2) -> (isMaximizing ? -1 : 1) *
                    (moveCache.getOrDefault(nextStates[m1].getHash(), emptyTT).score
                            - moveCache.getOrDefault(nextStates[m2].getHash(), emptyTT).score));
        } else if (sortMoves) {
            moveSearchOrder = new Integer[state.getMoveCount()];
            nextStates = new MutableGameState[state.getMoveCount()];
            for (int i = 0; i < state.getMoveCount(); i++) {
                moveSearchOrder[i] = i;
                if (pools[depth][i] != null) nextStates[i] = state.loadMoveTo(pools[depth][i], i);
                else nextStates[i] = pools[depth][i] = state.makeMove(i);
                state.undoMove();
            }
            Arrays.sort(moveSearchOrder, (m1, m2) -> (isMaximizing ? -1 : 1) *
                    (nextStates[m1].getEvaluation() - nextStates[m2].getEvaluation()));
        } else {
            nextStates = null;
            moveSearchOrder = null;
        }

        int bestScore = isMaximizing ? Integer.MIN_VALUE / 2 : Integer.MAX_VALUE / 2;
        int score;
        MutableGameState nextState = null;
        for (int i = 0; i < state.getMoveCount(); i++) {
            if (sortMoves) {
                state.makeMoveOnlyBoard(moveSearchOrder[i]);
                nextState = nextStates[moveSearchOrder[i]];
            } else if (depth + 1 != maxDepth) {
                if (pools[depth][0] != null) nextState = state.loadMoveTo(pools[depth][0], i);
                else pools[depth][0] = nextState = state.makeMove(i);
            } else state.makeMoveOnlyBoardEval(i);
            if (depth + 1 == maxDepth) score = evaluate(state, true);
            else score = minimaxScore(nextState, depth + 1, maxDepth, !isMaximizing, alpha, beta);
            state.undoMove();
            if (stopSearch) return 0;
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                if (isMaximizing) alpha = score;
                else beta = score;
                if (beta <= alpha) break;
            }
        }
        if (isMaximizing ? bestScore >= Integer.MAX_VALUE / 2 - 256 :
                bestScore <= -Integer.MAX_VALUE / 2 + 256)
            bestScore -= (isMaximizing ? 1 : -1);
        thisEntry.score = bestScore;
        thisEntry.depthRemaining = maxDepth - depth;
        moveCache.put(hashKey, thisEntry);
        return bestScore;
    }
}