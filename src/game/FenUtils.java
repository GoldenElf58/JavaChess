package game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        String path = "/game/start_positions.txt";
        InputStream resource = FenUtils.class.getResourceAsStream(path);
        if (resource == null) throw new IllegalStateException("Missing starting positions: " + path);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            String fen = reader.lines().skip(i).findFirst()
                    .orElseThrow(() -> new IndexOutOfBoundsException("No starting position at index " + i));
            return fenToGameState(fen);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
