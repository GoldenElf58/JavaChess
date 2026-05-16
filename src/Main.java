import app.Arrow;
import app.Colors;
import app.Div;
import app.SoundHandler;
import eval.BotV3UsePartialSearch;
import utils.Watch;
import eval.BotVTest;
import game.FenUtils;
import game.GameState;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static java.lang.Math.*;
import static utils.Watch.time;

public class Main extends Application {

    private static final double DEFAULT_SCENE_WIDTH = 720;
    private static final double DEFAULT_SCENE_HEIGHT = 480;
    private static final double MIN_SCENE_WIDTH = 292;
    private static final double MIN_SCENE_HEIGHT = 292;

    static final double ARROW_HEAD_LINE_RATIO = 0.2;

    static final double ARROW_HEAD_SMALL_RATIO = 0.5;
    static final double ARROW_STROKE_SMALL_RATIO = 0.04;
    static final double LINE_WIDTH_SMALL_RATIO = 0.135;
    static final double CIRCLE_RING_SMALL_RATIO = 0.05;
    static final double EDIT_OPACITY = 1;

    static final double ARROW_HEAD_RATIO = 0.55;
    static final double ARROW_STROKE_RATIO = 0.05;
    static final double LINE_WIDTH_RATIO = 0.15;
    static final double CIRCLE_RING_RATIO = 0.0833333333;

    static final String btnStyle = "-fx-background-color: #333333; -fx-text-fill: white; " +
            "-fx-border-color: transparent;";
    static final String btnHoverStyle = "-fx-background-color: #555555; -fx-text-fill: white; " +
            "-fx-border-color: transparent;";
    static final String btnClickStyle = "-fx-background-color: #777777; -fx-text-fill: white; " +
            "-fx-border-color: transparent;";
    static final String tfStyle = "-fx-background-color: #333333; -fx-text-fill: white; " +
            "-fx-border-color: transparent;";

    private final Map<Integer, Image> imageCache = new HashMap<>();
    private static double previousWidth = DEFAULT_SCENE_WIDTH;
    private static double previousHeight = DEFAULT_SCENE_HEIGHT;

    private Scene scene;
    private Group pencilMarkings;
    private Scene pencilScene;
    private GameState gameState;
    private List<GameState> gameStateHistory;
    private List<GameState> gameStateFuture;
    private List<byte[]> moveHistory;
    private List<byte[]> moveFuture;
    private WritableImage pencilImage;

    private Rectangle[] squares;
    private Circle[] circles;
    private ImageView[] pieces;
    private final Rectangle[] borders = new Rectangle[2];

    private Button btnDeepTest;
    private TextField tfAllottedTime;
    private Div div;
    private Text infoText;

    private final double[] mousePose = {-1, -1};
    private int specialStartSquare = -1;
    private int selectedSquare = -1;
    private int fromSquare = -1;
    private int toSquare = -1;
    private boolean dragging = true;
    private boolean firstSelection = false;
    private boolean shown = false;
    private boolean command = false;
    private boolean shift = false;
    private volatile boolean appOpen = true;
    private double pValue;
    private int totalCurrDepth = 0;
    private int totalTestDepth = 0;
    private int totalCurrMoves = 0;
    private int totalTestMoves = 0;

    // ==============================
    // Parameters
    // ==============================
    private static final boolean runBenchmarkOnly = false;
    private static boolean whitePlayerHuman = true;
    private static boolean blackPlayerHuman = true;
    private static final boolean debug = false;
    private static final boolean verbose = false;
    private static double allottedTime = 1.0;
    private static boolean deepTest = false;
    private static int testIdx = 0;
    private static int currWins = 0;
    private static int testWins = 0;
    private static int draws = 0;
    private static final int N = 50;
    private static final int warmup = N / 10;
    private static final int maxDepth = 4;
    private static final boolean useCurrBot = true;
    private static final boolean useTestBot = false;
    private static final boolean useBot1Move = true;
    private static final boolean useTestBotMove = false;

    private final BotV3UsePartialSearch currBot = new BotV3UsePartialSearch(false, true);
    private final BotVTest testBot = new BotVTest(false, true);

    @SuppressWarnings({"UnnecessaryModifier", "unused"})
    public static void main(String[] args) {
        if (runBenchmarkOnly) runBenchmark();
        else launch(args);
        System.exit(0);
    }

    private static void runBenchmark() {
        GameState gameState;
        double lastUpdate = -1;
        long lastTime = -1;
        long totalHalfMoves = 0;
        long[] perGame = new long[N];
        long[] oldTimes = new long[N];
        long[] newTimes = new long[N];
        BotV3UsePartialSearch currBot = new BotV3UsePartialSearch(false, false);
        BotVTest testBot = new BotVTest(false, false);
        Random random = new Random();
        Watch watch = new Watch();
        Watch oldWatch = new Watch();
        Watch newWatch = new Watch();
        int totalBars = 20;
        System.out.printf("\r0%% [%s] [0s]", "░".repeat(totalBars));
        if (maxDepth > 3) {
            for (int i = 0; i < (warmup + 1) * maxDepth; i++) {
                gameState = new GameState();
                gameState.computeMoves();
                int movesThisGame = 0;
                while (gameState.isInProgress()) {
                    movesThisGame++;
                    int move = random.nextInt(gameState.getMoveCount());
                    oldWatch.start();
                    if (useCurrBot) currBot.iterativeDeepening(gameState, 3);
                    oldWatch.stop();
                    newWatch.start();
                    if (useTestBot) testBot.iterativeDeepening(gameState, 3);
                    newWatch.stop();
                    gameState = gameState.makeMove(move);
                    gameState.computeMoves();
                    if (Main.debug) {
                        if (Main.verbose) {
                            IO.println(gameState);
                            gameState.printMoves();
                        }
                        System.out.printf("Half moves: %,d%n", movesThisGame);
                    }
                }
                currBot.clearCache();
                testBot.clearCache();
            }
        }
        for (int i = 0; i < N + warmup; i++) {
            if (i == warmup) watch.start();
            gameState = new GameState();
            gameState.computeMoves();
            int movesThisGame = 0;
            while (gameState.isInProgress()) {
                movesThisGame++;
                int move = random.nextInt(gameState.getMoveCount());
                oldWatch.start();
                if (useCurrBot)
                    move = useBot1Move ? currBot.iterativeDeepening(gameState, maxDepth) : move;
                oldWatch.stop();
                newWatch.start();
                if (useTestBot)
                    move = useTestBotMove ? testBot.iterativeDeepening(gameState, maxDepth) : move;
                newWatch.stop();
                gameState = gameState.makeMove(move);
                gameState.computeMoves();
                if (Main.debug) {
                    if (Main.verbose) {
                        IO.println(gameState);
                        gameState.printMoves();
                    }
                    System.out.printf("Half moves: %,d%n", movesThisGame);
                }
            }
            double progress = ((double) (i + 1) / (N + warmup)) * 100;
            long time = watch.getElapsedTimeSeconds();
            if (progress - lastUpdate >= 1 || lastTime - time >= 1) {
                int filledBars = (int) (progress / 100 * totalBars);
                String bar = "█".repeat(filledBars) + "░".repeat(totalBars - filledBars);
                lastTime = time;
                System.out.printf("\r%.0f%% [%s] [%ss]", progress, bar, lastTime);
                lastUpdate = (int) progress;
            }
            currBot.clearCache();
            testBot.clearCache();
            if (i >= warmup) {
                oldTimes[i - warmup] = oldWatch.getElapsedTimeNanos();
                newTimes[i - warmup] = newWatch.getElapsedTimeNanos();
                perGame[i - warmup] = movesThisGame;
                totalHalfMoves += movesThisGame;
            }
            oldWatch.reset();
            newWatch.reset();
        }
        watch.stop();
        IO.println("\n");
        System.out.printf("Max depth: %d%n", maxDepth);
        System.out.printf("Use Curr Bot: %b%n", useCurrBot);
        System.out.printf("Use Test Bot: %b%n", useTestBot);
        System.out.printf("Old time Average: %s%n", time(Arrays.stream(oldTimes).sum() / N));
        System.out.printf("New time Average: %s%n", time(Arrays.stream(newTimes).sum() / N));
        System.out.printf("Old time Standard Deviation: %s%n", time(round(stdDev(oldTimes))));
        System.out.printf("New time Standard Deviation: %s%n", time(round(stdDev(newTimes))));
        double[] ciOld = confidenceInterval95(oldTimes);
        double[] ciNew = confidenceInterval95(newTimes);
        System.out.printf("Old time 95%% CI: (%s, %s)%n", time(round(ciOld[0])), time(round(ciOld[1])));
        System.out.printf("New time 95%% CI: (%s, %s)%n", time(round(ciNew[0])), time(round(ciNew[1])));
        System.out.printf("Half moves: %,d%n", totalHalfMoves);
        System.out.printf("Games: %,d%n", N);
        System.out.printf("Time: %s%n", watch);
        System.out.printf("Average time per half-move: %s%n",
                time(watch.getElapsedTimeNanos() / totalHalfMoves));
        System.out.printf("Average time per game: %s%n", time(watch.getElapsedTimeNanos() / N));

        double mean = totalHalfMoves / (double) N;
        double[] ci = confidenceInterval95(perGame);

        System.out.printf("Average half-moves/game: %.2f%n", mean);
        System.out.printf("95%% CI (half-moves/game): (%.2f, %.2f)%n", ci[0], ci[1]);
    }

    public static double[] confidenceInterval95(long[] data) {
        double mean = Arrays.stream(data).average().orElse(0.0);
        double sd = stdDev(data);
        double margin = 1.96 * sd / Math.sqrt(data.length);
        double ciLower = mean - margin;
        double ciUpper = mean + margin;
        return new double[]{ciLower, ciUpper};
    }

    public static double stdDev(long[] data) {
        double mean = Arrays.stream(data).average().orElse(0.0);
        double sumSq = 0.0;
        for (long v : data) {
            double d = v - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / (data.length - 1));
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Chess");
        stage.setMinWidth(MIN_SCENE_WIDTH);
        stage.setMinHeight(MIN_SCENE_HEIGHT);

        Group root = new Group();
        pencilMarkings = new Group();
        scene = new Scene(root, 1080, 720, Color.grayRgb(0));
        pencilScene = new Scene(pencilMarkings, DEFAULT_SCENE_WIDTH, DEFAULT_SCENE_HEIGHT,
                Color.TRANSPARENT);
        SoundHandler.loadSounds();

        pencilImage = new WritableImage(3840, 2160);
        pencilScene.snapshot(pencilImage);
        gameState = new GameState();
        gameStateHistory = new Stack<>();
        gameStateFuture = new Stack<>();
        moveHistory = new Stack<>();
        moveFuture = new Stack<>();

        initializeNodes(root);

        div = new Div(4);
        div.add(tfAllottedTime = getAllottedTimeTF());
        div.add(getWhitePlayerButton());
        div.add(getBlackPlayerButton());
        div.add(btnDeepTest = getDeepTestButton());
        root.getChildren().addAll(div.getElements());
        root.getChildren().add(infoText = getInfoText());

        stage.setScene(scene);
        stage.show();
        scene.getRoot().requestFocus();

        gameState.computeMoves();
        if (!whitePlayerHuman) makeBotMoveAsync();

        scene.setOnMousePressed(this::onMousePressed);
        scene.setOnMouseDragged(this::onMouseDragged);
        scene.setOnMouseClicked(this::onMouseClicked);
        scene.setOnMouseMoved(this::onMouseMoved);
        scene.setOnKeyPressed(this::onKeyPressed);
        scene.setOnKeyReleased(this::onKeyReleased);
        stage.setOnCloseRequest(_ -> appOpen = false);

        scene.widthProperty().addListener(_ -> updatePositions());
        scene.heightProperty().addListener(_ -> updatePositions());
        updatePositions();

        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                loop();
            }
        };
        gameLoop.start();
    }

    private @NotNull TextField getAllottedTimeTF() {
        TextField allottedTimeTF = new TextField();
        allottedTimeTF.setTranslateX(10);
        allottedTimeTF.setTranslateY(150);
        allottedTimeTF.setPrefHeight(30);
        allottedTimeTF.setPrefWidth(100);
        allottedTimeTF.setStyle(tfStyle);
        allottedTimeTF.setText(String.valueOf(allottedTime));
        allottedTimeTF.deselect();
        allottedTimeTF.setOnAction(_ -> {
            if (!deepTest) allottedTime = Double.parseDouble(allottedTimeTF.getText());
            IO.println("Allotted Time Set To: " + time((long) (allottedTime * 1e9), 3));
        });
        allottedTimeTF.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) scene.getRoot().requestFocus();
        });
        return allottedTimeTF;
    }

    private @NotNull Button getBlackPlayerButton() {
        Button blackPlayer = new Button(blackPlayerHuman ? "Black Human" : "Black Bot");
        blackPlayer.setTranslateX(10);
        blackPlayer.setTranslateY(200);
        blackPlayer.setPrefHeight(30);
        blackPlayer.setPrefWidth(100);
        blackPlayer.setStyle(btnStyle);
        blackPlayer.setOnMouseEntered(_ -> {
            blackPlayer.setStyle(btnHoverStyle);
            scene.setCursor(Cursor.HAND);
        });
        blackPlayer.setOnMouseExited(_ -> {
            blackPlayer.setStyle(btnStyle);
            scene.setCursor(Cursor.DEFAULT);
        });
        blackPlayer.setOnAction(_ -> {
            blackPlayer.setStyle(btnHoverStyle);
            if (!deepTest) blackPlayerHuman = !blackPlayerHuman;
            if (!blackPlayerHuman && gameState.getColor() == -1) makeBotMoveAsync();
            blackPlayer.setText(blackPlayerHuman ? "Black Human" : "Black Bot");
        });
        blackPlayer.setOnMousePressed(_ -> blackPlayer.setStyle(btnClickStyle));
        return blackPlayer;
    }

    private @NotNull Button getWhitePlayerButton() {
        Button whitePlayer = new Button(whitePlayerHuman ? "White Human" : "White Bot");
        whitePlayer.setTranslateX(10);
        whitePlayer.setTranslateY(250);
        whitePlayer.setPrefHeight(30);
        whitePlayer.setPrefWidth(100);
        whitePlayer.setStyle(btnStyle);
        whitePlayer.setOnMouseEntered(_ -> {
            whitePlayer.setStyle(btnHoverStyle);
            scene.setCursor(Cursor.HAND);
        });
        whitePlayer.setOnMouseExited(_ -> {
            whitePlayer.setStyle(btnStyle);
            scene.setCursor(Cursor.DEFAULT);
        });
        whitePlayer.setOnAction(_ -> {
            whitePlayer.setStyle(btnHoverStyle);
            if (!deepTest) whitePlayerHuman = !whitePlayerHuman;
            if (!whitePlayerHuman && gameState.getColor() == 1) makeBotMoveAsync();
            whitePlayer.setText(whitePlayerHuman ? "White Human" : "White Bot");
        });
        whitePlayer.setOnMousePressed(_ -> whitePlayer.setStyle(btnClickStyle));
        return whitePlayer;
    }

    private @NotNull Button getDeepTestButton() {
        Button deepTest = new Button(Main.deepTest ? "Stop" : "Deep Test");
        deepTest.setTranslateX(10);
        deepTest.setTranslateY(300);
        deepTest.setPrefHeight(30);
        deepTest.setPrefWidth(100);
        deepTest.setStyle(btnStyle);
        deepTest.setOnMouseEntered(_ -> {
            deepTest.setStyle(btnHoverStyle);
            scene.setCursor(Cursor.HAND);
        });
        deepTest.setOnMouseExited(_ -> {
            deepTest.setStyle(btnStyle);
            scene.setCursor(Cursor.DEFAULT);
        });
        deepTest.setOnAction(_ -> {
            deepTest.setStyle(btnHoverStyle);
            Main.deepTest = !Main.deepTest;
            whitePlayerHuman = !Main.deepTest;
            blackPlayerHuman = !Main.deepTest;
            allottedTime = 0.1;
            testIdx = 0;
            tfAllottedTime.setText(String.valueOf(allottedTime));
            deepTest.setText(Main.deepTest ? "Stop" : "Deep Test");
            currBot.setPrinting(!Main.deepTest);
            testBot.setPrinting(!Main.deepTest);
            if (Main.deepTest) {
                currBot.clearCache();
                testBot.clearCache();
                currWins = 0;
                testWins = 0;
                draws = 0;
                gameStateFuture.clear();
                gameStateHistory.clear();
                moveHistory.clear();
                infoText.setText("White Wins: 0\nBlack Wins: 0\nDraws: 0\nP-Value: 1.0000");
                gameState = FenUtils.getFenGameState(testIdx);
                gameState.computeMoves();
                makeBotMoveAsync();
            } else infoText.setText("");
        });
        deepTest.setOnMousePressed(_ -> deepTest.setStyle(btnClickStyle));
        return deepTest;
    }

    private @NotNull Text getInfoText() {
        Text infoText = new Text();
        infoText.setTranslateX(610);
        infoText.setTranslateY(25);
        infoText.setWrappingWidth(100);
        infoText.setFill(Color.WHITE);
        infoText.setFont(new Font(14));
        return infoText;
    }

    private void updateInfoText() {
        if (!deepTest) return;
        infoText.setText(String.format("""
                            Curr Wins: %s
                            Test Wins: %s
                            Draws: %s
                            P-Value: %.4f
                            
                            Curr Depth: %.2f
                            Test Depth: %.2f
                            Curr Color: %s
                            Test Color: %s""",
                currWins, testWins, draws, pValue,
                (double) totalCurrDepth / totalCurrMoves,
                (double) totalTestDepth / totalTestMoves,
                2 * (testIdx % 2) - 1 == -1 ? "White" : "Black",
                2 * (testIdx % 2) - 1 == 1 ? "White" : "Black"));
    }

    public void loop() {
        displayBoard();

        if (!gameState.isInProgress() && !shown && !deepTest) {
            SoundHandler.playSound("game-end");
            IO.println(gameState);
            IO.println((switch (gameState.getWinner()) {
                case 0 -> "Draw";
                case 1 -> "White wins";
                case -1 -> "Black wins";
                default -> throw
                        new IllegalStateException("Unexpected value: " + gameState.getWinner());
            }));
            shown = true;
        }
    }

    private void onKeyReleased(KeyEvent e) {
        switch (e.getCode()) {
            case COMMAND -> command = false;
            case SHIFT -> shift = false;
            default -> {
            }
        }
    }

    private void onKeyPressed(KeyEvent e) {
        switch (e.getCode()) {
            case Q -> System.exit(0);
            case LEFT, A, J -> undo();
            case RIGHT, D, L -> redo();
            case Z -> {
                if (command) {
                    if (shift) redo();
                    else undo();
                }
            }
            case Y -> {
                if (command) redo();
            }
            case COMMAND -> command = true;
            case SHIFT -> shift = true;
            default -> {
            }
        }
    }

    private void undo() {
        if (gameStateHistory.isEmpty()) {
            SoundHandler.playSound("click");
            return;
        }
        shown = false;
        SoundHandler.playSound(moveHistory.getLast(), gameStateHistory.getLast(), gameState);
        gameStateFuture.add(gameState);
        gameState = gameStateHistory.removeLast();
        moveFuture.add(moveHistory.removeLast());
        gameState.computeMoves();
        setMoveSquares(moveHistory.isEmpty() ? null : moveHistory.getLast());
    }

    private void redo() {
        if (gameStateFuture.isEmpty()) {
            SoundHandler.playSound("click");
            return;
        }
        SoundHandler.playSound(moveFuture.getLast(), gameState, gameStateFuture.getLast());
        gameStateHistory.add(gameState);
        gameState = gameStateFuture.removeLast();
        moveHistory.add(moveFuture.removeLast());
        gameState.computeMoves();
        setMoveSquares(moveHistory.isEmpty() ? null : moveHistory.getLast());
    }

    private void initializeNodes(Group root) {
        squares = new Rectangle[64];
        circles = new Circle[64];
        pieces = new ImageView[64];
        double width = scene.getWidth();
        double height = scene.getHeight();
        double length = height / 8;
        for (int i = 0; i < 64; i++) {
            double x = getX(width, height, i);
            squares[i] = new Rectangle(x, floorDiv(i, 8) * length, length, length);
            squares[i].setFill(Colors.getSquareBaseColor(((i / 8) + (i % 8)) % 2 == 0));
            pieces[i] = new ImageView(new WritableImage(1, 1));
            pieces[i].setX(x);
            pieces[i].setY(floorDiv(i, 8) * length);
            circles[i] = new Circle(x + length / 2, floorDiv(i, 8) * length + length / 2,
                    length / 8);
            circles[i].setFill(Color.BLACK);
        }

        borders[0] = new Rectangle(0, 0, (width - height) / 2, height);
        borders[1] = new Rectangle(width - (width - height) / 2, 0, (width - height) / 2, height);
        ImageView iv = new ImageView(pencilImage);
        iv.setOpacity(0.6);

        root.getChildren().addAll(squares);
        root.getChildren().addAll(circles);
        root.getChildren().addAll(pieces);
        root.getChildren().addAll(borders);
        root.getChildren().add(iv);

    }

    private void onMousePressed(MouseEvent e) {
        int square = getSquare(scene, (int) e.getX(), (int) e.getY());
        if (e.getButton() == MouseButton.SECONDARY) {
            selectedSquare = -1;
            dragging = false;
            if (square == -1) return;
            specialStartSquare = square;
            scene.setCursor(Cursor.HAND);
            addBlank(pencilMarkings.getChildren());
            addBlank(pencilMarkings.getChildren());
            drawRing(scene, pencilMarkings, square);
            pencilScene.snapshot(pencilImage);
            return;
        }
        if (e.getButton() == MouseButton.BACK) undo();
        else if (e.getButton() == MouseButton.FORWARD) redo();
        if (e.getButton() != MouseButton.PRIMARY) return;
        pencilMarkings.getChildren().clear();
        pencilScene.snapshot(pencilImage);
        if (square == -1) return;
        boolean canSelect = canSelectSquare(gameState, selectedSquare, square);
        if (!canSelect) {
            firstSelection = true;
            selectedSquare = -1;
            return;
        }
        byte[] move = gameState.findMove(selectedSquare, square);
        if (move == null) {
            dragging = true;
            mousePose[0] = e.getX();
            mousePose[1] = e.getY();
            if (selectedSquare != square) firstSelection = true;
            scene.setCursor(Cursor.CLOSED_HAND);
            selectedSquare = square;
            return;
        }
        makeMove(move);
        if (gameState.isWhiteMove() ? !whitePlayerHuman : !blackPlayerHuman)
            makeBotMoveAsync();
        firstSelection = true;
        selectedSquare = -1;
    }

    private void onMouseDragged(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) {
            int square = getSquare(scene, (int) e.getX(), (int) e.getY());
            dragging = false;
            if (square == -1) return;
            scene.setCursor(Cursor.HAND);
            if (specialStartSquare == square)
                drawRing(scene, pencilMarkings, square);
            else drawArrow(scene, pencilMarkings, specialStartSquare, square);
            pencilScene.snapshot(pencilImage);
            return;
        }
        if (e.getButton() != MouseButton.PRIMARY) return;
        mousePose[0] = e.getX();
        mousePose[1] = e.getY();
        dragging = selectedSquare != -1;
    }

    private void onMouseClicked(MouseEvent e) {
        scene.getRoot().requestFocus();
        int currSquare = getSquare(scene, (int) e.getX(), (int) e.getY());
        double length = min(scene.getWidth(), scene.getHeight()) / 8;
        if (e.getButton() == MouseButton.SECONDARY) {
            ObservableList<Node> children = pencilMarkings.getChildren();
            if (children.isEmpty()) return;
            if (children.getLast() instanceof Group) {
                children.removeLast();
                double offset = min(scene.getWidth(), scene.getHeight()) / 16;
                if (currSquare == specialStartSquare) {
                    Circle circle = new Circle(getX(scene, currSquare) + offset,
                            getY(scene, currSquare) + offset,
                            offset - length * CIRCLE_RING_RATIO / 2);
                    circle.setFill(Color.TRANSPARENT);
                    circle.setStrokeWidth(length * CIRCLE_RING_RATIO);
                    for (Node child : children.reversed()) {
                        if (child instanceof Circle && equals((Circle) child, circle)) {
                            children.removeLast();
                            child.setVisible(!child.isVisible());
                            child.setManaged(!child.isManaged());
                            break;
                        }
                    }
                } else {
                    children.removeLast();
                    Arrow arrow = new Arrow(
                            getX(scene, specialStartSquare) + offset,
                            getY(scene, specialStartSquare) + offset,
                            getX(scene, currSquare) + offset,
                            getY(scene, currSquare) + offset,
                            length * ARROW_HEAD_LINE_RATIO);
                    Iterator<Node> it = children.iterator();
                    while (it.hasNext()) {
                        Node child = it.next();
                        if (child instanceof Arrow &&
                                ((Arrow) child).equals(new Arrow(
                                        getX(scene, specialStartSquare) + offset,
                                        getY(scene, specialStartSquare) + offset,
                                        getX(scene, currSquare) + offset,
                                        getY(scene, currSquare) + offset))) {
                            it.remove();
                        }
                        if (child instanceof Line &&
                                equals((Line) child, new Line(
                                        arrow.getStartX(), arrow.getStartY(),
                                        arrow.getX3(), arrow.getY3()))) {
                            it.remove();
                        }
                    }
                }
            }
            if (children.isEmpty()) {
                pencilScene.snapshot(pencilImage);
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            if (children.getLast() instanceof Line) {
                ((Line) children.getLast()).setStrokeWidth(length * LINE_WIDTH_RATIO);
                children.getLast().setOpacity(1);
                ((Arrow) children.get(children.size() - 2))
                        .setStrokeWidth(length * ARROW_STROKE_RATIO);
                ((Arrow) children.get(children.size() - 2))
                        .setArrowHeadSize(length * ARROW_HEAD_RATIO);
                children.get(children.size() - 2).setOpacity(1);
            } else if (children.getLast() instanceof Circle &&
                    ((Circle) children.getLast()).getStrokeWidth() ==
                            length * CIRCLE_RING_SMALL_RATIO) {
                ((Circle) children.getLast()).setStrokeWidth(length * CIRCLE_RING_RATIO);
                children.getLast().setOpacity(1);
            }
            pencilScene.snapshot(pencilImage);
            scene.setCursor(Cursor.DEFAULT);
            return;
        }
        if (e.getButton() != MouseButton.PRIMARY) return;
        if (gameState.isWhiteMove() ? !whitePlayerHuman : !blackPlayerHuman) return;
        if (!dragging) return;
        dragging = false;
        pieces[selectedSquare].setX(getX(scene, selectedSquare));
        pieces[selectedSquare].setY(getY(scene, selectedSquare));
        scene.setCursor(Cursor.DEFAULT);
        int square = getSquare(scene, (int) mousePose[0], (int) mousePose[1]);
        if (square == -1) return;
        boolean canSelect = canSelectSquare(gameState, selectedSquare, square);
        if (!canSelect) {
            selectedSquare = -1;
            firstSelection = true;
            return;
        }
        byte[] move = gameState.findMove(selectedSquare, square);
        if (move == null) {
            if (selectedSquare == square) {
                scene.setCursor(Cursor.OPEN_HAND);
                selectedSquare = firstSelection ? square : -1;
                firstSelection = !firstSelection;
                return;
            }
            firstSelection = true;
            selectedSquare = -1;
            return;
        }
        makeMove(move);
        if (gameState.isWhiteMove() ? !whitePlayerHuman : !blackPlayerHuman)
            makeBotMoveAsync();
        selectedSquare = -1;
    }

    private void makeMove(int moveIdx) {
        makeMove(gameState.getMove(moveIdx));
    }

    private void setMoveSquares(byte[] move) {
        if (move == null) {
            fromSquare = -1;
            toSquare = -1;
            return;
        }
        if (move[0] == -1) {
            fromSquare = move[1];
            toSquare = move[1] + move[2] * 2;
            return;
        }
        if (move[0] == -2) {
            fromSquare = move[1];
            toSquare = move[1] - 8 * gameState.getColor();
            return;
        }
        if (move[0] == -3) {
            fromSquare = move[1];
            toSquare = move[1] - gameState.getColor() * 8 + move[2];
            return;
        }
        if (move[0] <= -4) {
            fromSquare = move[1];
            toSquare = move[2];
            return;
        }
        fromSquare = move[0];
        toSquare = move[1];
    }

    private void makeMove(byte[] move) {
        setMoveSquares(move);
        gameStateHistory.add(gameState);
        gameStateFuture.clear();
        moveHistory.add(move);
        gameState = gameState.makeMove(move);
        if (!deepTest) SoundHandler.playSound(move, gameStateHistory.getLast(), gameState);
        gameState.computeMoves();
    }

    private double getPValue(int wins, int draws, int losses) {
        int n = wins + draws + losses;
        if (n == 0) return 1.0;
        double score = (wins + 0.5 * draws) / n;
        double z = (score - 0.5) / Math.sqrt(0.25 / n);

        return 2 * (1 - normalCDF(Math.abs(z)));
    }

    private static double normalCDF(double x) {
        return 0.5 * (1 + erf(x / Math.sqrt(2)));
    }

    private static double erf(double x) {
        double t = 1.0 / (1.0 + 0.3275911 * Math.abs(x));

        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;

        double poly = (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t;
        double result = 1 - poly * Math.exp(-x * x);

        return x >= 0 ? result : -result;
    }

    private void makeBotMoveAsync() {
        if (!gameState.isInProgress() || !appOpen) {
            if (deepTest) {
                if (gameState.getWinner() == 2 * (testIdx % 2) - 1) testWins++;
                else if (gameState.getWinner() != 0) currWins++;
                else draws++;
                testIdx++;
                pValue = getPValue(currWins, draws, testWins);
                IO.println("Curr Wins: " + currWins + " Test Wins: " + testWins +
                        " Draws: " + draws + " P-Value: " + String.format("%.4f", pValue) +
                        " Curr Depth: " + currBot.getLastDepth() +
                        " Test Depth: " + testBot.getLastDepth());
                updateInfoText();
                if (testIdx >= 1000) {
                    deepTest = false;
                    testIdx = 0;
                    whitePlayerHuman = true;
                    blackPlayerHuman = true;
                    currBot.setPrinting(true);
                    testBot.setPrinting(true);
                    btnDeepTest.setText("Deep Test");
                    return;
                }
                gameStateFuture.clear();
                gameStateHistory.clear();
                moveHistory.clear();
                currBot.clearCache();
                testBot.clearCache();
                gameState = FenUtils.getFenGameState(testIdx / 2);
                gameState.computeMoves();
                makeBotMoveAsync();
            }
            return;
        }
        selectedSquare = -1;
        new Thread(() -> {
            makeMove((deepTest && (gameState.getColor() == 2 * (testIdx % 2) - 1) ? testBot :
                    currBot).getMove(gameState, allottedTime));
            if (gameState.getColor() == 2 * (testIdx % 2) - 1) {
                totalCurrDepth += currBot.getLastDepth();
                totalCurrMoves++;
            } else {
                totalTestDepth += testBot.getLastDepth();
                totalTestMoves++;
            }
            updateInfoText();
            gameState.computeMoves();
            if (gameState.isWhiteMove() ? !whitePlayerHuman : !blackPlayerHuman)
                makeBotMoveAsync();
        }).start();
    }

    private void onMouseMoved(MouseEvent e) {
        mousePose[0] = e.getX();
        mousePose[1] = e.getY();
        dragging = false;
        if (selectedSquare != -1) {
            pieces[selectedSquare].setX(getX(scene, selectedSquare));
            pieces[selectedSquare].setY(getY(scene, selectedSquare));
            firstSelection = false;
        }
        int square = getSquare(scene, (int) e.getX(), (int) e.getY());
        if (square == -1 || !canSelectSquare(gameState, selectedSquare, square)) {
            scene.setCursor(Cursor.DEFAULT);
            return;
        }
        if (selectedSquare == -1) {
            scene.setCursor(Cursor.OPEN_HAND);
            return;
        }
        scene.setCursor(gameState.findMove(selectedSquare, square) == null ?
                Cursor.OPEN_HAND : Cursor.HAND);
    }

    public void updatePositions() {
        double width = scene.getWidth();
        double height = scene.getHeight();
        double length = Math.min(width, height) / 8;
        div.positionElements(scene);
        if ((width - height) / 2 < 120)
            infoText.setVisible(false);
        else {
            infoText.setTranslateX((width - height) / 2 + height + 10);
            infoText.setWrappingWidth(clamp((width - height) / 2 - 20, 100,
                    clamp(120 * height / 480, 120, 220)));
            infoText.setFont(new Font(clamp(height / 480 * 14, 12, 20)));
        }
        if (width < 292 - 28) {
            IO.println(width + " " + height);
            width = 292 - 28;
        }
        if (height < 292 - 28) {
            IO.println(width + " " + height);
            height = 292 - 28;
            length = height / 8;
        }
        borders[0].setHeight(scene.getHeight());
        borders[0].setWidth((width - height) / 2);
        borders[1].setHeight(scene.getHeight());
        borders[1].setWidth((width - height) / 2);
        borders[1].setX(width - (width - height) / 2);
        for (int i = 0; i < 64; i++) {
            double x = getX(width, height, i);
            double y = getY(width, height, i);
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
        for (Node child : pencilMarkings.getChildren()) {
            if (child instanceof Circle) {
                int square = getSquare(previousWidth, previousHeight,
                        (int) ((Circle) child).getCenterX(), (int) ((Circle) child).getCenterY());
                ((Circle) child).setCenterX(getX(width, height, square) + length / 2);
                ((Circle) child).setCenterY(getY(width, height, square) + length / 2);
                ((Circle) child).setStrokeWidth(length * CIRCLE_RING_RATIO);
                ((Circle) child).setRadius(length / 2 - ((Circle) child).getStrokeWidth() / 2);
            } else if (child instanceof Line) {
                int startSquare = getSquare(previousWidth, previousHeight,
                        (int) ((Line) child).getStartX(), (int) ((Line) child).getStartY());
                int endSquare = getSquare(previousWidth, previousHeight,
                        (int) ((Line) child).getEndX(), (int) ((Line) child).getEndY());
                Arrow arrow = new Arrow(getX(scene, startSquare) + length / 2,
                        getY(scene, startSquare) + length / 2, getX(scene, endSquare) + length / 2,
                        getY(scene, endSquare) + length / 2, length * ARROW_HEAD_LINE_RATIO);
                ((Line) child).setStartX(arrow.getStartX());
                ((Line) child).setStartY(arrow.getStartY());
                ((Line) child).setEndX(arrow.getX3());
                ((Line) child).setEndY(arrow.getY3());
                ((Line) child).setStrokeWidth(length * LINE_WIDTH_RATIO);
            } else if (child instanceof Arrow) {
                int startSquare = getSquare(previousWidth, previousHeight,
                        (int) ((Arrow) child).getStartX(), (int) ((Arrow) child).getStartY());
                int endSquare = getSquare(previousWidth, previousHeight,
                        (int) ((Arrow) child).getEndX(), (int) ((Arrow) child).getEndY());
                ((Arrow) child).setStartX(getX(width, height, startSquare) + length / 2);
                ((Arrow) child).setStartY(getY(width, height, startSquare) + length / 2);
                ((Arrow) child).setEndX(getX(width, height, endSquare) + length / 2);
                ((Arrow) child).setEndY(getY(width, height, endSquare) + length / 2);
                ((Arrow) child).setStrokeWidth(length * ARROW_STROKE_RATIO);
                ((Arrow) child).setArrowHeadSize(length * ARROW_HEAD_RATIO);
            }
        }
        pencilScene.snapshot(pencilImage);
        previousWidth = width;
        previousHeight = height;
    }

    public void displayBoard() {
        for (int i = 0; i < 64; i++) {
            displayTile(i);
            displayPiece(i);
        }
    }

    private void displayTile(int i) {
        int mouseSquare = getSquare(scene, (int) mousePose[0], (int) mousePose[1]);
        double squareWidth = min(scene.getWidth(), scene.getHeight()) / 8;
        int pieceType = gameState.getBoard()[i];
        Rectangle sq = squares[i];
        circles[i].setVisible(false);
        circles[i].setRadius(pieceType == 0 ? squareWidth / 7 : squareWidth / 1.8);

        boolean lightSquare = ((i / 8) + (i % 8)) % 2 == 0;
        sq.setFill(Colors.getSquareBaseColor(lightSquare));
        if (i == selectedSquare) {
            sq.setFill(Colors.getSquareSelectedColor(lightSquare));
        } else if (gameState.findMove(selectedSquare, i) != null) {
            if (mouseSquare == i) sq.setFill(Colors.getSquareHoverColor(lightSquare));
            else if (pieceType == 0) {
                circles[i].setVisible(true);
                circles[i].setFill(Colors.getSquareSelectedColor(lightSquare));
            } else {
                sq.setFill(Colors.getSquareHoverColor(lightSquare));
                circles[i].setVisible(true);
                circles[i].setFill(Colors.getSquareBaseColor(lightSquare));
                circles[i].toBack();
                sq.toBack();
            }
        } else if (i == fromSquare || i == toSquare) {
            sq.setFill(Colors.getSquareMoveColor(lightSquare));
        }
    }

    private void displayPiece(int i) {
        int pieceType = gameState.getBoard()[i];
        double squareWidth = min(scene.getWidth(), scene.getHeight()) / 8;
        ImageView piece = pieces[i];
        if (pieceType == 0) {
            piece.setVisible(false);
            return;
        }
        piece.setVisible(true);
        piece.setImage(imageCache.computeIfAbsent(pieceType, c ->
                new Image(new File("src" + "/piece_images/" + c + ".png").toURI().toString())));
        if (i == selectedSquare && dragging) {
            piece.setX(mousePose[0] - squareWidth / 2);
            piece.setY(mousePose[1] - squareWidth / 2);
            piece.toFront();
        }
    }

    public static void drawArrow(Scene scene, Group pencilMarkings, int square1, int square2) {
        double squareWidth = min(scene.getWidth(), scene.getHeight()) / 8;
        Arrow arrow = new Arrow(getX(scene, square1) + squareWidth / 2,
                getY(scene, square1) + squareWidth / 2, getX(scene, square2) + squareWidth / 2,
                getY(scene, square2) + squareWidth / 2, squareWidth * ARROW_HEAD_LINE_RATIO);
        arrow.setDrawTail(false);
        arrow.setStrokeWidth(squareWidth * ARROW_STROKE_SMALL_RATIO);
        arrow.setStroke(Colors.PENCIL_COLOR);
        arrow.setFill(Colors.PENCIL_COLOR);
        arrow.setStrokeLineCap(StrokeLineCap.ROUND);
        arrow.setOpacity(EDIT_OPACITY);

        Line line = new Line(arrow.getStartX(), arrow.getStartY(), arrow.getX3(), arrow.getY3());
        line.setStrokeWidth(squareWidth * LINE_WIDTH_SMALL_RATIO);
        line.setStroke(Colors.PENCIL_COLOR);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.setOpacity(EDIT_OPACITY);
        line.setOpacity(EDIT_OPACITY);

        arrow.setArrowHeadSize(squareWidth * ARROW_HEAD_SMALL_RATIO);

        ObservableList<Node> children = pencilMarkings.getChildren();
        children.removeLast();
        children.removeLast();
        for (Node child : children) {
            if (child instanceof Arrow && arrow.equals((Arrow) child)) {
                addBlank(children);
                addBlank(children);
                return;
            }
        }
        children.add(arrow);
        children.add(line);
    }

    public static void drawRing(Scene scene, Group pencilMarkings, int square) {
        double squareWidth = min(scene.getWidth(), scene.getHeight()) / 8;
        Circle circle = new Circle(getX(scene, square) + squareWidth / 2,
                getY(scene, square) + squareWidth / 2,
                squareWidth / 2 - squareWidth * CIRCLE_RING_RATIO / 2);
        circle.setFill(Color.TRANSPARENT);
        circle.setStrokeWidth(squareWidth * CIRCLE_RING_SMALL_RATIO);
        circle.setStroke(Colors.PENCIL_COLOR);
        circle.setOpacity(EDIT_OPACITY);

        ObservableList<Node> children = pencilMarkings.getChildren();
        children.removeLast();
        children.removeLast();
        for (Node child : children.reversed()) {
            if (child instanceof Circle && equals((Circle) child, circle)) {
                addBlank(children);
                addBlank(children);
                return;
            }
        }
        children.add(circle);
        addBlank(children);
    }

    public static void addBlank(ObservableList<Node> children) {
        Group blank = new Group();
        blank.setVisible(false);
        blank.setManaged(false);
        children.add(blank);
    }

    public static boolean equals(Circle c1, Circle c2) {
        return c1.getCenterX() == c2.getCenterX() && c1.getCenterY() == c2.getCenterY()
                && c1.getStrokeWidth() == c2.getStrokeWidth();
    }

    public static boolean equals(Line l1, Line l2) {
        return l1.getStartX() == l2.getStartX() && l1.getStartY() == l2.getStartY() &&
                l1.getEndX() == l2.getEndX() && l1.getEndY() == l2.getEndY();
    }

    public static int getSquare(Scene scene, int x, int y) {
        return getSquare(scene.getWidth(), scene.getHeight(), x, y);
    }

    public static int getSquare(double width, double height, int x, int y) {
        int length = (int) min(width, height) / 8;
        int row = width > height ? floorDiv(y, length) :
                floorDiv(y - (int) (height - width) / 2, length);
        int col = width > height ? floorDiv(x - (int) (width - height) / 2, length) :
                floorDiv(x, length);
        if (row < 0 || row > 7 || col < 0 || col > 7) return -1;
        return row * 8 + col;
    }

    public static boolean canSelectSquare(GameState gameState, int from, int to) {
        if (gameState.isWhiteMove() ? !whitePlayerHuman : !blackPlayerHuman) return false;
        boolean isColor = gameState.getBoard()[to] * gameState.getColor() > 0;
        if (isColor) return true;
        if (to == from || !gameState.isInProgress()) return false;
        return gameState.findMove(from, to) != null;
    }

    public static double getX(Scene scene, int idx) {
        return getX(scene.getWidth(), scene.getHeight(), idx);
    }

    public static double getY(Scene scene, int idx) {
        return getY(scene.getWidth(), scene.getHeight(), idx);
    }

    public static double getX(double width, double height, int idx) {
        return width > height ? (width - height) / 2 + (idx % 8) * height / 8 :
                idx % 8 * width / 8;
    }

    public static double getY(double width, double height, int idx) {
        return width > height ? floorDiv(idx, 8) * height / 8 :
                (height - width) / 2 + floorDiv(idx, 8) * width / 8;
    }
}
