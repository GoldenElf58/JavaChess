package eval;

import game.GameState;

public interface Bot {
    void setPrinting(boolean print);

    void clearCache();

    int getMove(GameState state, double allottedTime);

    int iterativeDeepening(GameState state, int maxDepth);

    int getLastDepth();
}
