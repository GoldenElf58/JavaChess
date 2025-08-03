import java.util.Random;


public class Main {
    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
        GameState gameState;
        Random random = new Random();
        int halfMoves = 0;
        int N = 1000;
        int moveChoice;
        long startTime = System.nanoTime();
        for (int i = 0; i < N; i++) {
            gameState = new GameState();
            while (!gameState.isWinner()) {
                gameState.computeMoves();
                if (gameState.getMoveCount() == 0) {
                    break;
                }
                moveChoice = random.nextInt(gameState.getMoveCount());
                gameState = gameState.makeMove(gameState.getMove(moveChoice));
                halfMoves++;
            }
        }
        long endTime = System.nanoTime();
        System.out.printf("Half moves: %,d%n", halfMoves);
        System.out.printf("Games: %,d%n", N);
        System.out.printf("Time: %,d ms%n", (endTime - startTime) / 1_000_000);
        System.out.printf("Average time (per half move): %,d ns%n", (endTime - startTime) / halfMoves);
    }
}