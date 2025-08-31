import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Bot {

    private int score;
    private final HashMap<Integer, Integer> moveCache = new HashMap<>();
    private final boolean log;

    public Bot(boolean log) {
        this.log = log;
    }

    private int evaluate(GameState state) {
        if (!state.isInProgress()) return state.getWinner() * Integer.MAX_VALUE;
        int score = 0;
        for (int i = 0; i < 64; i++) {
            int piece = state.getBoard()[i];
            score += PieceSquareTables.getPieceSquareValue(piece, i, piece > 0);
        }
        return score;
    }

    public int[] getMove(GameState state, int allottedTime) {
        return iterativeDeepening(state, allottedTime);
    }

    public int[] iterativeDeepening(GameState state, int allottedTime) {
        Watch watch = new Watch();
        watch.start();
        final int[] depth = {1};
        final int[][] move = {{0, 0, 0}};
        Thread thread = new Thread(() -> {
        });
        score = 0;
        while (watch.getElapsedTimeMillis() < allottedTime) {
            if (!thread.isAlive()) {
                if (score == Integer.MAX_VALUE || score == Integer.MIN_VALUE + 1) break;
                thread = new Thread(() -> {
                    move[0] = minimaxMove(state, depth[0], state.isWhiteMove());
                    depth[0]++;
                });
                thread.start();
            }
        }
        thread.interrupt();
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
                writer.println(depth[0]);
            } catch (IOException e) {
                System.out.println("Failed to append to file depths.txt");
            }
        }
        System.out.println("Depth: " + depth[0]);
        System.out.println("Score: " + score);
        return move[0];
    }

    private int hashCode(GameState state, int depth) {
        return Arrays.hashCode(new boolean[]{state.whiteQueen, state.whiteKing,
                state.blackQueen, state.blackKing}) ^ Arrays.hashCode(state.getBoard()) ^ depth;
    }

    private int[] minimaxMove(GameState state, int depth, boolean isMaximizing) {
        state.computeMoves();
        if (!state.isInProgress()) return new int[]{0, 0, 0};
        if (depth == 0) return new int[]{0, 0, 0};

        int bestScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int bestMoveIdx = 0;
        for (int i = 0; i < state.getMoveCount(); i++) {
            int score = minimaxScore(state.makeMove(state.getMove(i)), depth - 1,
                    !isMaximizing, Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMoveIdx = i;
            }
        }
        score = bestScore;
        return state.getMove(bestMoveIdx);
    }

    private int minimaxScore(GameState state, int depth, boolean isMaximizing, int alpha,
                             int beta) {
        if (depth == 0) return evaluate(state);
        state.computeMoves();
        if (!state.isInProgress()) return evaluate(state);
        if (moveCache.containsKey(hashCode(state, depth)))
            return moveCache.get(hashCode(state, depth));

        int bestScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int i = 0; i < state.getMoveCount(); i++) {
            int score = minimaxScore(state.makeMove(state.getMove(i)), depth - 1,
                    !isMaximizing, alpha, beta);
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                if (isMaximizing) alpha = max(alpha, score);
                else beta = min(beta, score);
                if (beta <= alpha) break;
            }
        }
        moveCache.put(hashCode(state, depth), bestScore);
        return bestScore;
    }
}
