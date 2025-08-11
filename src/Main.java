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
        int halfMoves = 0;
        int N = 100_000;
        int moveChoice;
        Watch watch = new Watch();
        watch.start();
        for (int i = 0; i < N; i++) {
            gameState = new GameState();
            gameState.computeMoves();
            while (gameState.isInProgress()) {
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
        System.out.printf("Average time: %,d ns%n", watch.getElapsedTimeNanos() / halfMoves);
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

//        final long[] startTime = {System.currentTimeMillis()};
//        Random rand = new Random();
//        final boolean[] shown = {false};
        AtomicInteger selectedSquare = new AtomicInteger(-1);
        // After setting up your squares and adding them to root:
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                GameState gameState = gameStates[0];
                scene.setOnMouseClicked(e -> {
                    int square = getSquare(scene, (int) e.getX(), (int) e.getY());
                    boolean canSelect = canSelectSquare(gameState, selectedSquare.get(), square);
                    if (canSelect) {
                        int[] move = findMove(gameState, selectedSquare.get(), square);
                        if (move != null) {
                            gameStates[0] = gameState.makeMove(move);
                            for (int i = 0; i < gameState.getMoveCount(); i++) {
                                int[] move2 = gameState.getMove(i);
                                for (int j = 0; j < 3; j++) {
                                    System.out.print(move2[j] + " ");
                                }
                                System.out.println();
                            }
                            selectedSquare.set(-1);
                        } else selectedSquare.set(square);
                    } else selectedSquare.set(-1);
                    System.out.println("Selected Square: " + selectedSquare.get());
                });
                displayBoard(scene, gameState, squares, pieces, selectedSquare.get());

//                if (System.currentTimeMillis() - startTime[0] > 5 && gameState.isInProgress()) {
//                    startTime[0] = System.currentTimeMillis();
//                    gameState.computeMoves();
//                    gameState = gameState.makeMove(gameState.getMove(rand.nextInt(
//                            gameState.getMoveCount())));
//                }
//                if (!gameState.isInProgress() && !shown[0]) {
//                    System.out.println(gameState);
//                    System.out.println(gameState.getWinner());
//                    System.out.println(gameState.getMoveCount());
//                    System.out.println(gameState.getColor());
//                    shown[0] = true;
//                }
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
        if (to == from) return false;
        return findMove(gameState, from, to) != null;
    }

    private static int[] findMove(GameState gameState, int from, int to) {
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
