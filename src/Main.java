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

import static java.lang.Math.floorDiv;

public class Main extends Application {
    public static void main(String[] args) {
//        launch(args);
        GameState gameState;
        Random random = new Random();
        int halfMoves = 0;
        int N = 10_000;
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
        final GameState[] gameState = {new GameState()};

        Color DARK_COLOR = Color.hsb(26.00, 0.47, 0.71);
        Color LIGHT_COLOR = Color.hsb(40.00, 0.23, 0.89);
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

        final long[] startTime = {System.currentTimeMillis()};
        Random rand = new Random();
        final boolean[] shown = {false};
        // After setting up your squares and adding them to root:
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
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

                    ImageView iv = pieces[i];
                    int code = gameState[0].getBoard()[i];
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
                if (System.currentTimeMillis() - startTime[0] > 5 && gameState[0].isInProgress()) {
                    startTime[0] = System.currentTimeMillis();
                    gameState[0].computeMoves();
                    gameState[0] = gameState[0].makeMove(gameState[0].getMove(rand.nextInt(
                            gameState[0].getMoveCount())));
                }
                if (!gameState[0].isInProgress() && !shown[0]) {
                    System.out.println(gameState[0]);
                    System.out.println(gameState[0].getWinner());
                    System.out.println(gameState[0].getMoveCount());
                    System.out.println(gameState[0].getColor());
                    shown[0] = true;
                }
            }
        };
        gameLoop.start();
    }
}
