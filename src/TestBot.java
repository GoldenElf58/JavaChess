import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

public class TestBot {

    private int score;
    private final HashMap<Long, TTEntry> moveCache = new HashMap<>();
    private final boolean log;
    private final TTEntry emptyTT = new TTEntry();
    private MutableGameState[][] pools = new MutableGameState[10][218];

    static class TTEntry {
        int score;
        int depthRemaining;
    }

    public TestBot(boolean log) {
        this.log = log;
    }

    public void clearCache() {
        moveCache.clear();
    }

    private int evaluate(MutableGameState state) {
        return state.getEvaluation();
    }

    public void iterativeDeepening(GameState state, int maxDepth) {
        Watch watch = new Watch();
        watch.start();
        clearCache();
        int depth = 1;
        score = 0;
        while (depth <= maxDepth) {
            if (score == Integer.MAX_VALUE / 2 || score == Integer.MIN_VALUE / 2) break;
            minimaxMove(state.asMutable(), depth, state.isWhiteMove());
            depth++;
        }
        if (log) {
            if (!new File("depths.txt").exists()) {
                System.out.println("Creating file depths.txt");
                try {
                    if (!new File("depths.txt").createNewFile())
                        System.out.println("Failed to create file depths.txt");
                } catch (IOException e) {
                    System.out.println("Failed to create file depths.txt");
                }
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter("depths.txt", true))) {
                writer.println(depth);
            } catch (IOException e) {
                System.out.println("Failed to append to file depths.txt");
            }
            System.out.println("Depth: " + depth);
            System.out.println("Score: " + score);
        }
    }

    public void minimaxMove(MutableGameState state, int depth, boolean isMaximizing) {
        assert depth > 0;
        state.computeMoves();
        if (!state.isInProgress()) return;
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
            Arrays.sort(moveSearchOrder, (m1, m2) -> (isMaximizing ? -1 : 1) *
                    (moveCache.getOrDefault(nextStates[m1].getHash(), emptyTT).score
                            - moveCache.getOrDefault(nextStates[m2].getHash(), emptyTT).score));
        } else {
            nextStates = null;
            moveSearchOrder = null;
        }

        MutableGameState nextState;
        if (depth > pools.length) pools = new MutableGameState[pools.length * 2][218];
        for (int i = 0; i < state.getMoveCount(); i++) {
            if (sortMoves) state.makeMoveOnlyBoard(moveSearchOrder[i]);
            nextState = sortMoves ? nextStates[moveSearchOrder[i]] : state.makeMove(i);
            int score = minimaxScore(nextState, 1, depth, !isMaximizing, alpha, beta);
            state.undoMove();
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                if (isMaximizing) alpha = score;
                else beta = score;
            }
        }
        this.score = bestScore;
    }

    private int minimaxScore(MutableGameState state, int depth, int maxDepth, boolean isMaximizing,
                             int alpha, int beta) {
        if (depth == maxDepth) return evaluate(state);
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
        if (sortMoves) {
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
                if (pools[depth][i] != null) nextState = state.loadMoveTo(pools[depth][i], i);
                else pools[depth][i] = nextState = state.makeMove(i);
            } else state.makeMoveOnlyBoardEval(i);
            if (depth + 1 == maxDepth) score = state.getCurEval();
            else score = minimaxScore(nextState, depth + 1, maxDepth, !isMaximizing, alpha, beta);
            state.undoMove();
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