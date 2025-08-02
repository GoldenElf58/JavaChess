public class Main {
    public static void main(String[] args) {
        GameState gameState = new GameState();
        for (int[] move : gameState.getMoves()) {
            for (int element : move) {
                System.out.print(element + " ");
            }
            System.out.println();
        }
        System.out.println(gameState);
    }
}