import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class FenUtils {
    void main() {
        Scanner s = new Scanner(System.in);
        while (true) {
            IO.print("Fen Number: ");
            int idx = s.nextInt();
            s.nextLine();
            IO.println(getFenGameState(idx));
            IO.print("Quit? (y/N) ");
            String quit = s.nextLine();
            if (quit.equalsIgnoreCase("y")) break;
        }
    }

    public static GameState getFenGameState(int i) {
        File fens = new File("src/start_positions.txt");
        String fen;
        try (FileReader fr = new FileReader(fens)) {
            fen = fr.readAllLines().get(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fenToGameState(fen);
    }

    public static GameState fenToGameState(String fen) {
        byte[] board = new byte[64];

        String[] fenParts = fen.split(" ");
        String boardPosition = fenParts[0];

        int squareIdx = 0;
        for (int i = 0; i < boardPosition.length(); i++) {
            char c = boardPosition.charAt(i);

            if (c == '/') continue;
            if (Character.isDigit(c)) {
                int emptySquares = Character.getNumericValue(c);
                squareIdx += emptySquares;
            } else {
                byte pieceValue = switch (Character.toLowerCase(c)) {
                    case 'p' -> 1;
                    case 'n' -> 2;
                    case 'b' -> 3;
                    case 'r' -> 4;
                    case 'q' -> 5;
                    case 'k' -> 6;
                    default -> 0;
                };

                if (Character.isUpperCase(c)) board[squareIdx] = pieceValue;
                else board[squareIdx] = (byte) -pieceValue;

                squareIdx++;
            }
        }

        boolean whiteMove = fenParts[1].equals("w");
        boolean whiteQueen = fenParts[2].contains("Q");
        boolean blackQueen = fenParts[2].contains("q");
        boolean whiteKing = fenParts[2].contains("K");
        boolean blackKing = fenParts[2].contains("k");
        int halfMoves = Integer.parseInt(fenParts[5]);
        byte halfMoveClock = Byte.parseByte(fenParts[4]);

        return new GameState(board, whiteQueen, whiteKing, blackQueen, blackKing, null, halfMoves,
                halfMoveClock, whiteMove, null, false, (byte) 0);
    }
}