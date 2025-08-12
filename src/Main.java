import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.floorDiv;

public class Main extends Application {

    static final Color DARK_COLOR = Color.rgb(181, 136, 99);
    static final Color LIGHT_COLOR = Color.rgb(240, 217, 181);
    static final Color DARK_SELECTED_COLOR = Color.rgb(100, 110, 64);
    static final Color LIGHT_SELECTED_COLOR = Color.rgb(130, 151, 105);
    static final Color DARK_HOVER_COLOR = Color.rgb(132, 121, 78);
    static final Color LIGHT_HOVER_COLOR = Color.rgb(174, 177, 135);

    private final Map<Integer, Image> imageCache = new HashMap<>();
    private static final Map<String, Media> soundCache = new HashMap<>();
    private static boolean soundsLoaded = false;

    public static void main(String[] args) {
        launch(args);
//        GameState gameState;
//        Random random = new Random();
//        int N = 10_000;
//        long totalHalfMoves = 0;
//        long[] perGame = new long[N];
//        int moveChoice;
//        Watch watch = new Watch();
//        watch.start();
//        for (int i = 0; i < N; i++) {
//            gameState = new GameState();
//            gameState.computeMoves();
//            int movesThisGame = 0;
//            while (gameState.isInProgress()) {
//                moveChoice = random.nextInt(gameState.getMoveCount());
//                gameState = gameState.makeMove(gameState.getMove(moveChoice));
//                movesThisGame++;
//                gameState.computeMoves();
//            }
//            perGame[i] = movesThisGame;
//            totalHalfMoves += movesThisGame;
//        }
//        watch.stop();
//        System.out.printf("Half moves: %,d%n", totalHalfMoves);
//        System.out.printf("Games: %,d%n", N);
//        System.out.printf("Time: %,d ms%n", watch.getElapsedTimeMillis());
//        System.out.printf("Average time: %,d ns%n", watch.getElapsedTimeNanos() / totalHalfMoves);
//
//        double mean = totalHalfMoves / (double) N;
//        double sumSq = 0.0;
//        for (long v : perGame) {
//            double d = v - mean;
//            sumSq += d * d;
//        }
//        double sd = Math.sqrt(sumSq / (N - 1));
//        double margin = 1.96 * sd / Math.sqrt(N);
//        double ciLower = mean - margin;
//        double ciUpper = mean + margin;
//
//        System.out.printf("Average half-moves/game: %.2f%n", mean);
//        System.out.printf("95%% CI (half-moves/game): [%.2f, %.2f]%n", ciLower, ciUpper);
    }

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root, 720, 480, Color.grayRgb(0));
        stage.setTitle("Chess");
        stage.setMinWidth(128);
        stage.setMinHeight(128);
        final GameState[] gameStates = {new GameState()};

        Rectangle[] squares = new Rectangle[64];
        Circle[] circles = new Circle[64];
        ImageView[] pieces = new ImageView[64];
        double width = scene.getWidth();
        double height = scene.getHeight();
        double length = height / 8;
        for (int i = 0; i < 64; i++) {
            double x = getX(width, height, i);
            squares[i] = new Rectangle(x, floorDiv(i, 8) * length, length, length);
            squares[i].setFill(((i / 8) + (i % 8)) % 2 == 1 ? DARK_COLOR : LIGHT_COLOR);
            pieces[i] = new ImageView(new WritableImage(1, 1));
            pieces[i].setX(x);
            pieces[i].setY(floorDiv(i, 8) * length);
            circles[i] = new Circle(x + length / 2, floorDiv(i, 8) * length + length / 2,
                    length / 8);
            circles[i].setFill(Color.BLACK);
        }
        root.getChildren().addAll(squares);
        root.getChildren().addAll(circles);
        root.getChildren().addAll(pieces);

        Rectangle[] borders = {
                new Rectangle(0, 0, (width - height) / 2, height),
                new Rectangle(width - (width - height) / 2, 0, (width - height) / 2, height),
        };
        root.getChildren().addAll(borders);

        stage.setScene(scene);
        stage.show();

        gameStates[0].computeMoves();

        final boolean[] shown = {false};
        AtomicInteger selectedSquare = new AtomicInteger(-1);
        double[] mousePose = {-1, -1};
        boolean[] dragging = {false};
        boolean[] firstSelection = {true};

        scene.setOnMousePressed(e -> {
            GameState gameState = gameStates[0];
            int square = getSquare(scene, (int) e.getX(), (int) e.getY());
            if (square == -1) return;
            boolean canSelect = canSelectSquare(gameState, selectedSquare.get(), square);
            if (!canSelect) {
                firstSelection[0] = true;
                selectedSquare.set(-1);
                return;
            }
            int[] move = findMove(gameState, selectedSquare.get(), square);
            if (move == null) {
                dragging[0] = true;
                mousePose[0] = e.getX();
                mousePose[1] = e.getY();
                if (selectedSquare.get() != square) firstSelection[0] = true;
                scene.setCursor(Cursor.CLOSED_HAND);
                selectedSquare.set(square);
                return;
            }
            gameStates[0] = gameState.makeMove(move);
            playSound(move, gameState, gameStates[0]);
            gameStates[0].computeMoves();
            firstSelection[0] = true;
            selectedSquare.set(-1);
        });

        scene.setOnMouseDragged(e -> {
            mousePose[0] = e.getX();
            mousePose[1] = e.getY();
            dragging[0] = selectedSquare.get() != -1;
        });

        scene.setOnMouseReleased(_ -> {
            if (!dragging[0]) return;
            dragging[0] = false;
            pieces[selectedSquare.get()].setX(getX(scene, selectedSquare.get()));
            pieces[selectedSquare.get()].setY(getY(scene, selectedSquare.get()));
            scene.setCursor(Cursor.DEFAULT);
            GameState gameState = gameStates[0];
            int square = getSquare(scene, (int) mousePose[0], (int) mousePose[1]);
            if (square == -1) return;
            boolean canSelect = canSelectSquare(gameState, selectedSquare.get(), square);
            if (!canSelect) {
                selectedSquare.set(-1);
                firstSelection[0] = true;
                return;
            }
            int[] move = findMove(gameState, selectedSquare.get(), square);
            if (move == null) {
                if (selectedSquare.get() == square) {
                    scene.setCursor(Cursor.OPEN_HAND);
                    selectedSquare.set(firstSelection[0] ? square : -1);
                    firstSelection[0] = !firstSelection[0];
                    return;
                }
                firstSelection[0] = true;
                selectedSquare.set(-1);
                return;
            }
            gameStates[0] = gameState.makeMove(move);
            playSound(move, gameState, gameStates[0]);
            gameStates[0].computeMoves();
            selectedSquare.set(-1);
        });

        scene.setOnMouseMoved(e -> {
            mousePose[0] = e.getX();
            mousePose[1] = e.getY();
            if (dragging[0]) return;
            GameState gameState = gameStates[0];
            int square = getSquare(scene, (int) e.getX(), (int) e.getY());
            if (square == -1 || !canSelectSquare(gameState, selectedSquare.get(), square)) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            if (selectedSquare.get() == -1) {
                scene.setCursor(Cursor.OPEN_HAND);
                return;
            }
            scene.setCursor(findMove(gameState, selectedSquare.get(), square) == null ?
                    Cursor.OPEN_HAND : Cursor.HAND);
        });

        scene.widthProperty().addListener(_ -> updatePositions(scene, squares, circles, pieces,
                borders));
        scene.heightProperty().addListener(_ -> updatePositions(scene, squares, circles, pieces,
                borders));
        updatePositions(scene, squares, circles, pieces, borders);

        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                GameState gameState = gameStates[0];

                displayBoard(scene, gameState, squares, circles, pieces, selectedSquare.get(),
                        mousePose, dragging[0]);

                if (!gameState.isInProgress() && !shown[0]) {
                    System.out.println(gameState);
                    System.out.printf((switch (gameState.getWinner()) {
                        case 0 -> "Draw";
                        case 1 -> "White wins";
                        case -1 -> "Black wins";
                        default ->
                                throw new IllegalStateException("Unexpected value: " + gameState.getWinner());
                    }));
                    shown[0] = true;
                }
            }
        };
        gameLoop.start();
    }

    public static void loadSounds() {
        String[] files = {"move-check", "move-opponent", "move-self", "capture", "castle", "promote"};
        for (String file : files) {
            Media media = new Media(new File("src/sounds/" + file + ".mp3").toURI().toString());
            soundCache.put(file, media);
        }
        soundsLoaded = true;
    }

    public static void playSound(String sound) {
        if (!soundsLoaded) loadSounds();
        new MediaPlayer(soundCache.get(sound)).play();
    }

    public static void playSound(int[] move, GameState gameStateNow, GameState gameStateMoved) {
        if (gameStateMoved.inCheck()) {
            playSound("move-check");
            return;
        }
        if (move[0] == -1) {
            playSound("castle");
            return;
        }
        if (move[0] == -2 || move[0] <= -4) {
            playSound("promote");
            return;
        }
        if (move[0] == -3) {
            playSound("capture");
            return;
        }
        int[] board = gameStateNow.getBoard();
        if (board[move[1]] == 0) {
            playSound(gameStateNow.isWhiteMove() ? "move-self" : "move-opponent");
            return;
        }
        playSound("capture");
    }

    public void updatePositions(Scene scene, Rectangle[] squares, Circle[] circles,
                                ImageView[] pieces, Rectangle[] borders) {
        double width = scene.getWidth();
        double height = scene.getHeight();
        double length = height / 8;
        borders[0].setHeight(scene.getHeight());
        borders[0].setWidth((width - height) / 2);
        borders[1].setHeight(scene.getHeight());
        borders[1].setWidth((width - height) / 2);
        borders[1].setX(width - (width - height) / 2);
        for (int i = 0; i < 64; i++) {
            double x = getX(width, height, i);
            double y = getY(height, i);
            Rectangle sq = squares[i];
            sq.setWidth(length);
            sq.setHeight(length);
            sq.setX(x);
            sq.setY(y);
            circles[i].setCenterX(x + length / 2);
            circles[i].setCenterY(y + length / 2);
            pieces[i].setX(x);
            pieces[i].setY(y);
            pieces[i].setFitHeight(length);
            pieces[i].setFitWidth(length);
        }
    }

    public void displayBoard(Scene scene, GameState gameState, Rectangle[] squares,
                             Circle[] circles, ImageView[] pieces, int selectedSquare,
                             double[] mousePose, boolean dragging) {
        double length = scene.getHeight() / 8;
        int mouseSquare = getSquare(scene, (int) mousePose[0], (int) mousePose[1]);
        for (int i = 0; i < 64; i++) {
            int pieceType = gameState.getBoard()[i];
            Rectangle sq = squares[i];

            circles[i].setVisible(false);
            circles[i].setRadius(pieceType == 0 ? length / 7 : length / 1.8);
            boolean lightSquare = ((i / 8) + (i % 8)) % 2 == 0;
            if (i == selectedSquare) {
                sq.setFill(getSelectedColor(lightSquare));
            } else if (findMove(gameState, selectedSquare, i) != null) {
                if (mouseSquare == i) sq.setFill(getHoverColor(lightSquare));
                else {
                    if (pieceType == 0) {
                        sq.setFill(getBaseColor(lightSquare));
                        circles[i].setVisible(true);
                        circles[i].setFill(getSelectedColor(lightSquare));
                    } else {
                        sq.setFill(getHoverColor(lightSquare));
                        circles[i].setVisible(true);
                        circles[i].setFill(getBaseColor(lightSquare));
                        circles[i].toBack();
                        sq.toBack();
                    }
                }
            } else sq.setFill(getBaseColor(lightSquare));

            ImageView piece = pieces[i];
            if (pieceType == 0) {
                piece.setVisible(false);
                continue;
            }
            piece.setVisible(true);
            piece.setImage(imageCache.computeIfAbsent(pieceType, c ->
                    new Image(new File("src" + "/piece_images/" + c + ".png").toURI().toString())));
            if (i == selectedSquare && dragging) {
                piece.setX(mousePose[0] - length / 2);
                piece.setY(mousePose[1] - length / 2);
                piece.toFront();
            }
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
        for (int i = 0; i < gameState.getMoveCount(); i++) {
            int[] move = gameState.getMove(i);
            if (move[0] >= 0) {
                if (move[0] == from && move[1] == to) {
                    return move;
                }
            } else {
                if (move[0] == -1) {
                    if (move[1] == from && (to == from + move[2] * 2 ||
                            (move[2] == 1 ? to == from + 3 : to == from - 4)))
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

    public static double getX(Scene scene, int idx) {
        return getX(scene.getWidth(), scene.getHeight(), idx);
    }

    public static double getY(Scene scene, int idx) {
        return getY(scene.getHeight(), idx);
    }

    public static double getX(double width, double height, int idx) {
        return (width - height) / 2 + (idx % 8) * height / 8;
    }

    public static double getY(double height, int idx) {
        return floorDiv(idx, 8) * height / 8;
    }

    public static Color getBaseColor(boolean light) {
        return light ? LIGHT_COLOR : DARK_COLOR;
    }

    public static Color getSelectedColor(boolean light) {
        return light ? LIGHT_SELECTED_COLOR : DARK_SELECTED_COLOR;
    }

    public static Color getHoverColor(boolean light) {
        return light ? LIGHT_HOVER_COLOR : DARK_HOVER_COLOR;
    }}
