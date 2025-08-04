import java.util.Random;


public class Main {
    public static void main(String[] args) {
        GameState gameState;
        Random random = new Random();
        int halfMoves = 0;
        int N = 10_000;
        int moveChoice;
        Watch watch = new Watch();
        watch.start();
        for (int i = 0; i < N; i++) {
            gameState = new GameState();
            while (!gameState.isWinner()) {
                gameState.computeMoves();
                moveChoice = random.nextInt(gameState.getMoveCount());
                gameState = gameState.makeMove(gameState.getMove(moveChoice));
                halfMoves++;
                gameState.computeMoves();
            }
        }
        watch.stop();
        System.out.printf("Half moves: %,d%n", halfMoves);
        System.out.printf("Games: %,d%n", N);
        System.out.printf("Time: %,d ms%n", watch.getElapsedTimeMillis());
        System.out.printf("Average time (per half move): %,d ns%n", watch.getElapsedTimeNanos() / halfMoves);
    }
}