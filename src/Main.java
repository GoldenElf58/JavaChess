import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Random;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.floorDiv;

public class Main extends Application {

    static final Color DARK_COLOR = Color.rgb(181, 136, 99);
    static final Color LIGHT_COLOR = Color.rgb(240, 217, 181);
    static final Color DARK_SELECTED_COLOR = Color.rgb(211, 116, 79);
    static final Color LIGHT_SELECTED_COLOR = Color.rgb(245, 157, 131);

    public static void main(String[] args) {
        launch(args);
        GameState gameState;
        Random random = new Random();
        int N = 10_000;
        long totalHalfMoves = 0;
        long[] perGame = new long[N];
        int moveChoice;
        Watch watch = new Watch();
        watch.start();
        for (int i = 0; i < N; i++) {
            gameState = new GameState();
            gameState.computeMoves();
            int movesThisGame = 0;
            while (gameState.isInProgress()) {
                moveChoice = random.nextInt(gameState.getMoveCount());
                gameState = gameState.makeMove(gameState.getMove(moveChoice));
                movesThisGame++;
                gameState.computeMoves();
            }
            perGame[i] = movesThisGame;
            totalHalfMoves += movesThisGame;
        }
        watch.stop();
        System.out.printf("Half moves: %,d%n", totalHalfMoves);
        System.out.printf("Games: %,d%n", N);
        System.out.printf("Time: %,d ms%n", watch.getElapsedTimeMillis());
        System.out.printf("Average time: %,d ns%n", watch.getElapsedTimeNanos() / totalHalfMoves);

        double mean = totalHalfMoves / (double) N;
        double sumSq = 0.0;
        for (long v : perGame) {
            double d = v - mean;
            sumSq += d * d;
        }
        double sd = Math.sqrt(sumSq / (N - 1));
        double margin = 1.96 * sd / Math.sqrt(N);
        double ciLower = mean - margin;
        double ciUpper = mean + margin;

        System.out.printf("Average half-moves/game: %.2f%n", mean);
        System.out.printf("95%% CI (half-moves/game): [%.2f, %.2f]%n", ciLower, ciUpper);
    }

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root, 720, 480, Color.grayRgb(0));
        stage.setTitle("Chess");
        final GameState[] gameStates = {new GameState()};

        Rectangle[] squares = new Rectangle[64];
        ImageView[] pieces = new ImageView[64];
        double width = scene.getWidth();
        double height = scene.getHeight();
        double length = height / 8;
        for (int i = 0; i < 64; i++) {
            squares[i] = new Rectangle((width - height) / 2 + (i % 8) * length,
                    floorDiv(i, 8) * length, length, length);
            squares[i].setFill(((i / 8) + (i % 8)) % 2 == 1 ? DARK_COLOR : LIGHT_COLOR);
            pieces[i] = new ImageView(new WritableImage(1, 1));
            root.getChildren().add(squares[i]);
            root.getChildren().add(pieces[i]);
        }

        stage.setScene(scene);
        stage.show();

        final boolean[] shown = {false};
        AtomicInteger selectedSquare = new AtomicInteger(-1);
        // After setting up your squares and adding them to root:
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                GameState gameState = gameStates[0];
                scene.setOnMouseClicked(e -> {
                    int square = getSquare(scene, (int) e.getX(), (int) e.getY());
                    boolean canSelect = canSelectSquare(gameState, selectedSquare.get(), square);
                    if (!canSelect) {
                        selectedSquare.set(-1);
                        return;
                    }
                    int[] move = findMove(gameState, selectedSquare.get(), square);
                    if (move == null) {
                        selectedSquare.set(square);
                        return;
                    }
                    gameStates[0] = gameState.makeMove(move);
                    gameStates[0].computeMoves();
                    for (int i = 0; i < gameStates[0].getMoveCount(); i++) {
                        System.out.println(gameState.moveRepr(gameStates[0].getMove(i)) + " | " +
                                gameStates[0].moveToString(gameStates[0].getMove(i)));
                    }
                    selectedSquare.set(-1);
                });
                displayBoard(scene, gameState, squares, pieces, selectedSquare.get());

                if (!gameState.isInProgress() && !shown[0]) {
                    System.out.println(gameState);
                    System.out.println("Winner: " + gameState.getWinner());
                    shown[0] = true;
                }
            }
        };
        gameLoop.start();
    }

    public void displayBoard(Scene scene, GameState gameState, Rectangle[] squares,
                             ImageView[] pieces, int selectedSquare) {
        double width = scene.getWidth();
        double height = scene.getHeight();
        double length = height / 8;
        for (int i = 0; i < 64; i++) {
            double x = (width - height) / 2 + (i % 8) * length;
            double y = floorDiv(i, 8) * length;
            Rectangle sq = squares[i];
            sq.setWidth(length);
            sq.setHeight(length);
            sq.setX(x);
            sq.setY(y);
            if (i == selectedSquare) {
                sq.setFill(((i / 8) + (i % 8)) % 2 == 1 ? DARK_SELECTED_COLOR :
                        LIGHT_SELECTED_COLOR);
            } else {
                sq.setFill(((i / 8) + (i % 8)) % 2 == 1 ? DARK_COLOR : LIGHT_COLOR);
            }

            ImageView iv = pieces[i];
            int code = gameState.getBoard()[i];
            if (code != 0) {
                var res = getClass().getResource("/piece_images/" + code + ".png");
                Image img = (res != null) ? new Image(res.toExternalForm())
                        : new Image(new File("src/piece_images/" + code + ".png")
                        .toURI().toString());
                iv.setImage(img);
            } else iv.setImage(new WritableImage(1, 1));
            iv.setX(x);
            iv.setY(y);
            iv.setFitWidth(length);
            iv.setFitHeight(length);
        }
    }

    public static int getSquare(Scene scene, int x, int y) {
        int length = (int) scene.getHeight() / 8;
        int width = (int) scene.getWidth();
        int row = floorDiv(y, length);
        int col = floorDiv(x - (int) (width - scene.getHeight()) / 2, length);
        if (row < 0 || row > 7 || col < 0 || col > 7) return -1;
        return row * 8 + col;
    }

    public static boolean canSelectSquare(GameState gameState, int from, int to) {
        boolean isColor = gameState.getBoard()[to] * gameState.getColor() > 0;
        if (isColor) return true;
        if (to == from || !gameState.isInProgress()) return false;
        return findMove(gameState, from, to) != null;
    }

    private static int[] findMove(GameState gameState, int from, int to) {
        if (to == from || from == -1) return null;
        gameState.computeMoves();
        for (int i = 0; i < gameState.getMoveCount(); i++) {
            int[] move = gameState.getMove(i);
            if (move[0] >= 0) {
                if (move[0] == from && move[1] == to) {
                    return move;
                }
            } else {
                if (move[0] == -1) {
                    if (move[1] == from && (to == from + move[2] * 2 || to == from + move[2] * 3))
                        return move;
                } else if (move[0] == -2) {
                    if (move[1] == from && from - 8 * gameState.getColor() == to) return move;
                } else if (move[0] == -3) {
                    if (move[1] == from && to == from - 8 * gameState.getColor() + move[2])
                        return move;
                } else {
                    if (move[1] == from && move[2] == to) return move;
                }
            }
        }
        return null;
    }
}
