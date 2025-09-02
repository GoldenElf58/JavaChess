import java.io.*;
import java.util.*;

public class Bot {

    private int score;
    private final List<HashMap<Long, Integer>> moveCache = new ArrayList<>();
    private final long[][] zobristTable = new long[65][13];
    private final boolean log;

    public Bot(boolean log) {
        initZobristTable();
        this.log = log;
    }

    public void clearCache() {
        moveCache.clear();
    }

    private void initZobristTable() {
        Random random = new Random();
        for (int i = 0; i < 65; i++) {
            for (int j = 0; j < 13; j++) {
                zobristTable[i][j] = random.nextLong();
            }
        }
    }

    private long zobristHash(GameState state) {
        long hash = 0;
        for (int i = 0; i < 64; i++) {
            int piece = state.getBoard()[i];
            if (piece == 0) continue;
            hash ^= zobristTable[i][piece + 6];
        }
        if (state.whiteKing) hash ^= zobristTable[64][0];
        if (state.whiteQueen) hash ^= zobristTable[64][1];
        if (state.blackKing) hash ^= zobristTable[64][2];
        if (state.blackQueen) hash ^= zobristTable[64][3];
        if (state.isWhiteMove()) hash ^= zobristTable[64][4];
        return hash;
    }

    private int evaluate(GameState state) {
        if (!state.isInProgress()) return state.getWinner() * Integer.MAX_VALUE / 2;
        int score = 0;
        int[] board = state.getBoard();
        for (int i = 0; i < 64; i++)
            score += PieceSquareTables.getPieceSquareValue(board[i], i);
        return score;
    }

    public int[] getMove(GameState state, double allottedTime) {
        return iterativeDeepening(state, allottedTime);
    }

    public int[] getMove(GameState state, int depth) {
        for (int i = 1; i < depth; i++)
            minimaxMove(state, i, state.isWhiteMove());
        return minimaxMove(state, depth, state.isWhiteMove());
    }

    public int[] getMoveNew(GameState state, int depth) {
        for (int i = 1; i < depth; i++)
            minimaxMove(state, i, state.isWhiteMove());
        return minimaxMoveNew(state, depth, state.isWhiteMove());
    }

    public int[] iterativeDeepening(GameState state, double allottedTime) {
        Watch watch = new Watch();
        watch.start();
        final int[] depth = {1};
        final int[][] move = {{0, 0, 0}};
        Thread thread = new Thread(() -> {
        });
        score = 0;
        while (watch.getElapsedTimeMillis() / 1000d < allottedTime) {
            if (!thread.isAlive()) {
                if (score == Integer.MAX_VALUE / 2 || score == Integer.MIN_VALUE / 2) break;
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

    private int[] minimaxMove(GameState state, int depth, boolean isMaximizing) {
        assert depth > 0;
        state.computeMoves();
        if (!state.isInProgress()) return new int[]{0, 0, 0};
        while (moveCache.size() < depth) moveCache.add(new HashMap<>());
        int bestScore = isMaximizing ? Integer.MIN_VALUE / 2 : Integer.MAX_VALUE / 2;
        int bestMoveIdx = 0;
        int alpha = Integer.MIN_VALUE / 2;
        int beta = Integer.MAX_VALUE / 2;

        Integer[] moveSearchOrder = new Integer[state.getMoveCount()];
        boolean sortMoves = depth > 2;
        if (sortMoves) {
            for (int i = 0; i < state.getMoveCount(); i++) moveSearchOrder[i] = i;
            Arrays.sort(moveSearchOrder, (m1, m2) -> (isMaximizing ? -1 : 1) *
                    (moveCache.get(depth - 2).getOrDefault(zobristHash(state.makeMove(m1)), 0)
                            - moveCache.get(depth - 2).getOrDefault(zobristHash(state.makeMove(m2)), 0)));
        }

        for (int i = 0; i < state.getMoveCount(); i++) {
            int score = minimaxScore(state.makeMove(state.getMove(sortMoves ?
                    moveSearchOrder[i] : i)), 1, depth, !isMaximizing, alpha, beta);
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMoveIdx = sortMoves ? moveSearchOrder[i] : i;
                if (isMaximizing) alpha = score;
                else beta = score;
            }
        }
        score = bestScore;
        return state.getMove(bestMoveIdx);
    }

    private int minimaxScore(GameState state, int depth, int maxDepth, boolean isMaximizing,
                             int alpha, int beta) {
        if (depth == maxDepth) return evaluate(state);
        long hashKey = zobristHash(state);
        if (moveCache.get(maxDepth - depth).containsKey(hashKey))
            return moveCache.get(maxDepth - depth).get(hashKey);
        state.computeMoves();
        if (!state.isInProgress()) return state.getWinner() * Integer.MAX_VALUE / 2;

        GameState[] gameStates = new GameState[state.getMoveCount()];
        if (maxDepth - depth > 2) {
            for (int i = 0; i < state.getMoveCount(); i++) {
                gameStates[i] = state.makeMove(i);
            }
            Arrays.sort(gameStates, (state1, state2) -> (isMaximizing ? -1 : 1) *
                    (moveCache.get(maxDepth - depth - 2).getOrDefault(zobristHash(state1), 0)
                            - moveCache.get(maxDepth - depth - 2).getOrDefault(zobristHash(state2), 0)));
        }

        int bestScore = isMaximizing ? Integer.MIN_VALUE / 2 : Integer.MAX_VALUE / 2;
        int score;
        for (int i = 0; i < state.getMoveCount(); i++) {
            score = minimaxScore(maxDepth - depth > 2 ? gameStates[i] : state.makeMove(i),
                    depth + 1, maxDepth, !isMaximizing, alpha, beta);
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                if (isMaximizing) alpha = score;
                else beta = score;
                if (beta <= alpha) break;
            }
        }
        moveCache.get(maxDepth - depth).put(hashKey, bestScore);
        return bestScore;
    }

    private int[] minimaxMoveNew(GameState state, int depth, boolean isMaximizing) {
        assert depth > 0;
        state.computeMoves();
        if (!state.isInProgress()) return new int[]{0, 0, 0};
        while (moveCache.size() < depth) moveCache.add(new HashMap<>());
        int bestScore = isMaximizing ? Integer.MIN_VALUE / 2 : Integer.MAX_VALUE / 2;
        int bestMoveIdx = 0;
        int alpha = Integer.MIN_VALUE / 2;
        int beta = Integer.MAX_VALUE / 2;

        Integer[] moveSearchOrder = new Integer[state.getMoveCount()];
        boolean sortMoves = depth > 2;
        if (sortMoves) {
            for (int i = 0; i < state.getMoveCount(); i++) moveSearchOrder[i] = i;
            Arrays.sort(moveSearchOrder, (m1, m2) -> (isMaximizing ? -1 : 1) *
                    (moveCache.get(depth - 2).getOrDefault(zobristHash(state.makeMove(m1)), 0)
                            - moveCache.get(depth - 2).getOrDefault(zobristHash(state.makeMove(m2)), 0)));
        }

        for (int i = 0; i < state.getMoveCount(); i++) {
            int score = minimaxScoreNew(state.makeMove(state.getMove(sortMoves ?
                    moveSearchOrder[i] : i)), 1, depth, !isMaximizing, alpha, beta);
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMoveIdx = sortMoves ? moveSearchOrder[i] : i;
                if (isMaximizing) alpha = score;
                else beta = score;
            }
        }
        score = bestScore;
        return state.getMove(bestMoveIdx);
    }

    private int minimaxScoreNew(GameState state, int depth, int maxDepth, boolean isMaximizing,
                             int alpha, int beta) {
        if (depth == maxDepth) return evaluate(state);
        long hashKey = zobristHash(state);
        if (moveCache.get(maxDepth - depth).containsKey(hashKey))
            return moveCache.get(maxDepth - depth).get(hashKey);
        state.computeMoves();
        if (!state.isInProgress()) return state.getWinner() * Integer.MAX_VALUE / 2;

        GameState[] gameStates = new GameState[state.getMoveCount()];
        if (maxDepth - depth > 2) {
            for (int i = 0; i < state.getMoveCount(); i++) {
                gameStates[i] = state.makeMove(i);
            }
            Arrays.sort(gameStates, (state1, state2) -> (isMaximizing ? -1 : 1) *
                    (moveCache.get(maxDepth - depth - 2).getOrDefault(zobristHash(state1), 0)
                            - moveCache.get(maxDepth - depth - 2).getOrDefault(zobristHash(state2), 0)));
        }

        int bestScore = isMaximizing ? Integer.MIN_VALUE / 2 : Integer.MAX_VALUE / 2;
        int score;
        for (int i = 0; i < state.getMoveCount(); i++) {
            score = minimaxScoreNew(maxDepth - depth > 2 ? gameStates[i] : state.makeMove(i),
                    depth + 1, maxDepth, !isMaximizing, alpha, beta);
            if (isMaximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                if (isMaximizing) alpha = score;
                else beta = score;
                if (beta <= alpha) break;
            }
        }
        moveCache.get(maxDepth - depth).put(hashKey, bestScore);
        return bestScore;
    }
}