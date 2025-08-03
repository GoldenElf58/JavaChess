import java.util.Random;


public class Main {
    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
        GameState gameState;
        Random random = new Random();
        int halfMoves = 0;
        int N = 100_000;
        int moveChoice;
        long startTime = System.nanoTime();
        for (int i = 0; i < N; i++) {
            gameState = new GameState();
            while (!gameState.isWinner()) {
//                int j = 0;
                gameState.computeMoves();
//                for (int[] move : moves) {
//                    System.out.print(j + ": ");
//                    for (int element : move) {
//                        System.out.print(element + " ");
//                    }
//                    System.out.println();
//                    j++;
//                }
//                System.out.println(gameState);
//                System.out.println("" + gameState.whiteKing + gameState.whiteQueen +
//                        gameState.blackKing + gameState.blackQueen);
//                System.out.print("Enter move: ");
                moveChoice = random.nextInt(gameState.getMoveCount());
//                do {
//                    moveChoice = random.nextInt(gameState.getMoveCount());
//                } while (moves[moveChoice * 4] == 0 && moves[moveChoice * 4 + 1] == 0);
//                int moveChoice = scanner.nextInt();
//                System.out.println();
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