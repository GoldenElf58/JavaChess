import app.Arrow;
import app.Colors;
import app.Div;
import app.SoundHandler;
import eval.Bot;
import eval.BotV9fLMR;
import game.FenUtils;
import game.GameState;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
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
import utils.Watch;

import java.io.File;
import java.util.*;

import static java.lang.Math.*;
import static java.lang.String.format;
import static utils.Watch.time;

public class Main extends Application {

    private static final double DEFAULT_SCENE_WIDTH = 1080;
    private static final double DEFAULT_SCENE_HEIGHT = 720;
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

    static final String btnStyle = "-fx-background-color: #303030; -fx-text-fill: #eeeeee; " +
            "-fx-border-color: transparent;";
    static final String btnHoverStyle = "-fx-background-color: #505050; -fx-text-fill: #eeeeee; " +
            "-fx-border-color: transparent;";
    static final String btnClickStyle = "-fx-background-color: #707070; -fx-text-fill: #eeeeee; " +
            "-fx-border-color: transparent;";
    static final String tfStyle = "-fx-background-color: #303030; -fx-text-fill: #eeeeee; " +
            "-fx-border-color: transparent;";

    private final Map<Integer, Image> imageCache = new HashMap<>();
    private static double previousWidth = DEFAULT_SCENE_WIDTH;
    private static double previousHeight = DEFAULT_SCENE_HEIGHT;

    private Scene scene;
    private Stage stage;
    private Group pencilMarkings;
    private Scene pencilScene;
    private GameState gameState;
    private List<GameState> gameStateHistory;
    private List<GameState> gameStateFuture;
    private List<byte[]> moveHistory;
    private List<byte[]> moveFuture;
    private WritableImage pencilImage;
    private Rectangle darkRect;

    private Rectangle[] squares;
    private Circle[] circles;
    private ImageView[] pieces;
    private final Rectangle[] borders = new Rectangle[2];

    private Button btnDeepTest, btnReset;
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
    private double llr;
    private double pConclusive;
    private final int[] currDepths = new int[64];
    private final int[] testDepths = new int[64];
    private boolean darkMode = false;
    private static boolean deepTest = false;
    private static boolean deepTestPaused = false;
    private static int testIdx = 0;
    private static int wins = 0;
    private static int draws = 0;
    private static int losses = 0;
    private static double lastScore = 0;
    private static int[] penta = new int[5];
    private static boolean promotionChoice = false;
    private static byte[] promotionMove;

    // ==============================
    // Parameters
    // ==============================
    private static final boolean runBenchmarkOnly = false;
    private static boolean whitePlayerHuman = true;
    private static boolean blackPlayerHuman = true;
    private static final boolean debug = false;
    private static final boolean verbose = false;
    private static double allottedTime = 1.0;
    private static final int N = 2;
    private static final int warmup = N / 10 + 1;
    private static final int maxDepth = 5;
    private static final boolean useCurrBot = true;
    private static final boolean useTestBot = true;
    private static final boolean useCurrBotMove = true;
    private static final boolean useTestBotMove = false;

    private static final Bot currBot = new BotV9fLMR(false, true);
    private static final Bot testBot = new BotV9fLMR(false, true);

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
        currBot.setPrinting(false);
        testBot.setPrinting(false);
        Random random = new Random();
        Watch watch = new Watch();
        Watch oldWatch = new Watch();
        Watch newWatch = new Watch();
        int totalBars = 20;
        System.out.printf("\r0%% [%s] [0s]", "░".repeat(totalBars));
        if (maxDepth > 3) {
            for (int i = 0; i < warmup * maxDepth * maxDepth; i++) {
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
                if (useCurrBot) {
                    move = useCurrBotMove ? currBot.iterativeDeepening(gameState, maxDepth) : move;
                    if (!useCurrBotMove) currBot.iterativeDeepening(gameState, maxDepth);
                }
                oldWatch.stop();
                newWatch.start();
                if (useTestBot) {
                    move = useTestBotMove ? testBot.iterativeDeepening(gameState, maxDepth) : move;
                    if (!useTestBotMove) testBot.iterativeDeepening(gameState, maxDepth);
                }
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
        long oldAverage = Arrays.stream(oldTimes).sum() / N;
        long newAverage = Arrays.stream(newTimes).sum() / N;
        double percent = (double) newAverage / oldAverage * 100;
        percent = round(percent * 1000) / 1000d;
        System.out.printf("Max depth: %d%n", maxDepth);
        System.out.printf("Use Curr Bot: %b%n", useCurrBot);
        System.out.printf("Use Test Bot: %b%n", useTestBot);
        System.out.printf("Use Curr Bot Move: %b%n", useCurrBotMove);
        System.out.printf("Use Test Bot Move: %b%n", useTestBotMove);
        System.out.printf("Curr Bot Name: %s%n", currBot);
        System.out.printf("Test Bot Name: %s%n", testBot);
        System.out.printf("Old time Average: %s%n", time(oldAverage));
        System.out.printf("New time Average: %s%n", time(newAverage));
        System.out.printf("Old time Standard Deviation: %s%n", time(round(stdDev(oldTimes))));
        System.out.printf("New time Standard Deviation: %s%n", time(round(stdDev(newTimes))));
        double[] ciOld = confidenceInterval95(oldTimes);
        double[] ciNew = confidenceInterval95(newTimes);
        System.out.printf("Old time 95%% CI: (%s, %s)%n", time(round(ciOld[0])), time(round(ciOld[1])));
        System.out.printf("New time 95%% CI: (%s, %s)%n", time(round(ciNew[0])), time(round(ciNew[1])));
        System.out.printf("Percent of Old: %s%%%n", percent);
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
        this.stage = stage;
        stage.setTitle("Chess");
        stage.setMinWidth(MIN_SCENE_WIDTH);
        stage.setMinHeight(MIN_SCENE_HEIGHT);
        double height = DEFAULT_SCENE_HEIGHT;
        double width = DEFAULT_SCENE_WIDTH;

        Group root = new Group();
        pencilMarkings = new Group();
        scene = new Scene(root, width, height, Color.grayRgb(0));
        pencilScene = new Scene(pencilMarkings, width, height, Color.TRANSPARENT);
        SoundHandler.loadSounds();

        pencilImage = new WritableImage(3840, 2160);
        pencilScene.snapshot(pencilImage);
        gameState = new GameState();
        gameStateHistory = new Stack<>();
        gameStateFuture = new Stack<>();
        moveHistory = new Stack<>();
        moveFuture = new Stack<>();

        initializeNodes(root);

        darkRect = new Rectangle((width - height) / 2, 0, (width - height) * 2,
                (width - height) * 2);
        darkRect.setOpacity(0);
        root.getChildren().add(darkRect);

        div = new Div(6);
        div.add(tfAllottedTime = getAllottedTimeTF());
        div.add(getBlackPlayerButton());
        div.add(getWhitePlayerButton());
        div.add(btnDeepTest = getDeepTestButton());
        div.add(getDarkModeButton());
        div.add(btnReset = getResetButton());
        div.positionElements(scene);
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
            blackPlayerHuman = !blackPlayerHuman;
            if (!deepTest && !deepTestPaused && !blackPlayerHuman && gameState.getColor() == -1)
                makeBotMoveAsync();
            blackPlayer.setText(blackPlayerHuman ? "Black Human" : "Black Bot");
        });
        blackPlayer.setOnMousePressed(_ -> blackPlayer.setStyle(btnClickStyle));
        return blackPlayer;
    }

    private @NotNull Button getWhitePlayerButton() {
        Button whitePlayer = new Button(whitePlayerHuman ? "White Human" : "White Bot");
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
            whitePlayerHuman = !whitePlayerHuman;
            if (!deepTest && !deepTestPaused && !whitePlayerHuman && gameState.getColor() == 1)
                makeBotMoveAsync();
            whitePlayer.setText(whitePlayerHuman ? "White Human" : "White Bot");
        });
        whitePlayer.setOnMousePressed(_ -> whitePlayer.setStyle(btnClickStyle));
        return whitePlayer;
    }

    private @NotNull Button getDeepTestButton() {
        Button deepTest = new Button(Main.deepTest ? "Stop" : "Deep Test");
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
            if (deepTestPaused) {
                deepTestPaused = false;
                Main.deepTest = true;
                whitePlayerHuman = false;
                blackPlayerHuman = false;
                allottedTime = 0.1;
                tfAllottedTime.setText(String.valueOf(allottedTime));
                deepTest.setText("Stop");
                btnReset.setText("Pause");
                currBot.setPrinting(false);
                testBot.setPrinting(false);
                updateInfoText();
                gameState.computeMoves();
                makeBotMoveAsync();
                return;
            }
            Main.deepTest = !Main.deepTest;
            whitePlayerHuman = true;
            blackPlayerHuman = true;
            allottedTime = 0.1;
            testIdx = 0;
            tfAllottedTime.setText(String.valueOf(allottedTime));
            deepTest.setText(Main.deepTest ? "Stop" : "Deep Test");
            currBot.setPrinting(!Main.deepTest);
            testBot.setPrinting(!Main.deepTest);
            if (Main.deepTest) {
                IO.println("Deep Test");
                IO.println("Curr Bot: " + currBot);
                IO.println("Test Bot: " + testBot);
                btnReset.setText("Pause");
                deepTestPaused = false;
                currBot.clearCache();
                testBot.clearCache();
                penta = new int[5];
                losses = 0;
                wins = 0;
                draws = 0;
                gameStateFuture.clear();
                gameStateHistory.clear();
                moveHistory.clear();
                updateInfoText();
                gameState = FenUtils.getFenGameState(testIdx);
                gameState.computeMoves();
                makeBotMoveAsync();
                return;
            }
            IO.println(currBot);
            printHistogram(currDepths);
            IO.println(testBot);
            printHistogram(testDepths);
            infoText.setText("");
            btnReset.setText("Reset Position");
        });
        deepTest.setOnMousePressed(_ -> deepTest.setStyle(btnClickStyle));
        return deepTest;
    }

    private @NotNull Button getDarkModeButton() {
        Button btnDarkMode = new Button(darkMode ? "Light Mode" : "Dark Mode");
        btnDarkMode.setStyle(btnStyle);
        btnDarkMode.setOnMouseEntered(_ -> {
            btnDarkMode.setStyle(btnHoverStyle);
            scene.setCursor(Cursor.HAND);
        });
        btnDarkMode.setOnMouseExited(_ -> {
            btnDarkMode.setStyle(btnStyle);
            scene.setCursor(Cursor.DEFAULT);
        });
        btnDarkMode.setOnAction(_ -> {
            btnDarkMode.setStyle(btnHoverStyle);
            darkMode = !darkMode;
            darkRect.setOpacity(darkMode ? 0.3 : 0);
            darkRect.toFront();
            btnDarkMode.setText(darkMode ? "Light Mode" : "Dark Mode");
        });
        btnDarkMode.setOnMousePressed(_ -> btnDarkMode.setStyle(btnClickStyle));
        return btnDarkMode;
    }

    private @NotNull Button getResetButton() {
        Button btnReset = new Button("Reset Position");
        btnReset.setStyle(btnStyle);
        btnReset.setOnMouseEntered(_ -> {
            btnReset.setStyle(btnHoverStyle);
            scene.setCursor(Cursor.HAND);
        });
        btnReset.setOnMouseExited(_ -> {
            btnReset.setStyle(btnStyle);
            scene.setCursor(Cursor.DEFAULT);
        });
        btnReset.setOnAction(_ -> {
            btnReset.setStyle(btnHoverStyle);
            if (deepTest) {
                btnReset.setText("Paused");
                btnDeepTest.setText("Resume");
                IO.println("Curr Bot: " + currBot);
                printHistogram(currDepths);
                IO.println("Test Bot: " + testBot);
                printHistogram(testDepths);
                deepTestPaused = true;
                deepTest = false;
                whitePlayerHuman = true;
                blackPlayerHuman = true;
                return;
            }
            if (deepTestPaused) return;
            btnReset.setText("Reset Position");
            gameState = new GameState();
            gameState.computeMoves();
            fromSquare = -1;
            toSquare = -1;
            gameStateFuture.clear();
            gameStateHistory.clear();
            moveHistory.clear();
            updateInfoText();
            if (!whitePlayerHuman) makeBotMoveAsync();
        });
        btnReset.setOnMousePressed(_ -> btnReset.setStyle(btnClickStyle));
        return btnReset;
    }

    private @NotNull Text getInfoText() {
        infoText = new Text();
        infoText.setTranslateX(610);
        infoText.setTranslateY(25);
        infoText.setWrappingWidth(100);
        infoText.setFill(Color.gray(.95));
        infoText.setFont(new Font(14));
        deepTest = true;
        updateInfoText();
        deepTest = false;
        return infoText;
    }

    private String evalToString(int eval, int depth) {
        return eval >= Integer.MAX_VALUE / 2 - depth * 2 ?
                "M" + (Integer.MAX_VALUE / 2 - eval) :
                eval <= Integer.MIN_VALUE / 2 + depth * 2 ?
                "M" + (eval - 1 - Integer.MIN_VALUE / 2) : "" + eval / 100f;
    }

    private void updateInfoTextDeepTest() {
        boolean tmp = Main.deepTest;
        Main.deepTest = true;
        updateInfoText();
        Main.deepTest = tmp;
    }

    private double trimmedMean(int[] histogram) {
        int total = Arrays.stream(histogram).sum();
        int sum = 0;
        int rollingTotal = 0;
        int lowBound = (int) (.025 * total);
        int highBound = (int) (.975 * total);
        for (int i = 0; i < histogram.length; i++) {
            if (rollingTotal < lowBound) {
                rollingTotal += histogram[i];
                if (rollingTotal < lowBound) continue;
                sum += i * (rollingTotal - lowBound);
                continue;
            }
            if ((rollingTotal + histogram[i]) > highBound) {
                sum += i * (highBound - rollingTotal);
                break;
            }
            rollingTotal += histogram[i];
            sum += i * histogram[i];
        }
        return (double) sum / total;
    }

    private void printHistogram(int[] histogram) {
        int total = Arrays.stream(histogram).sum();
        int rollingTotal = 0;
        IO.println(format("Average: %.3f", trimmedMean(histogram)));
        for (int i = 0; i < histogram.length; i++) {
            rollingTotal += histogram[i];
            if (histogram[i] / (double) total < .001) continue;
            System.out.printf("%s: %.1f%%%n", i, (double) histogram[i] / total * 100);
            if ((double) rollingTotal / total > .999) break;
        }
    }

    private void updateInfoText() {
        int eval = currBot.getLastEval();
        int depth = currBot.getLastDepth();
        if (!deepTest && !deepTestPaused) {
            infoText.setText(format("""
                    Depth: %d
                    Eval: %s
                    """, depth, evalToString(eval, depth)));
            return;
        }
        infoText.setText(format("""
                        Curr Wins: %s
                        Test Wins: %s
                        Draws: %s
                        
                        P-Value: %.4f
                        LLR: %.3f
                        P-Conclusive: %.4f
                        
                        Curr Depth: %.2f
                        Test Depth: %.2f
                        Curr Color: %s
                        Test Color: %s
                        
                        Eval: %s
                        
                        Curr Name: %s
                        Test Name: %s""",
                losses, wins, draws, pValue, llr, pConclusive,
                trimmedMean(currDepths), trimmedMean(testDepths),
                2 * (testIdx % 2) - 1 == -1 ? "White" : "Black",
                2 * (testIdx % 2) - 1 == 1 ? "White" : "Black", evalToString(eval, depth),
                currBot, testBot));
    }

    public void loop() {
        displayBoard();

        if (!gameState.isInProgress() && !shown && !deepTest && !deepTestPaused) {
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
            case ESCAPE -> stage.setFullScreen(false);
            default -> {
            }
        }
    }

    private void undo() {
        if (promotionChoice) {
            promotionChoice = false;
            return;
        }
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
        if (promotionChoice) {
            promotionChoice = false;
            int piece;
            byte color = gameState.getColor();
            int dest = promotionMove[0] == -2 ? promotionMove[1] - 8 * color : promotionMove[2];
            if (square == dest) piece = 5;
            else if (square == dest + 8 * color) piece = 2;
            else if (square == dest + 16 * color) piece = 4;
            else if (square == dest + 24 * color) piece = 3;
            else piece = 0;
            if (piece == 0) {
                displayBoard();
                return;
            }
            if (promotionMove[0] == -2) promotionMove[2] = (byte) (piece * color);
            else promotionMove[0] = (byte) (-2 - piece);
            makeMove(promotionMove);
            displayBoard();
            return;
        }
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
        } else if (move[0] <= -4 || move[0] == -2) {
            getPromotionMove(move);
            firstSelection = true;
            selectedSquare = -1;
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
        } else if (move[0] <= -4 || move[0] == -2) {
            getPromotionMove(move);
            firstSelection = true;
            selectedSquare = -1;
            return;
        }
        makeMove(move);
        if (gameState.isWhiteMove() ? !whitePlayerHuman : !blackPlayerHuman)
            makeBotMoveAsync();
        selectedSquare = -1;
    }

    private void getPromotionMove(byte[] move) {
        byte color = gameState.getColor();
        int dest = move[0] == -2 ? move[1] - 8 * color : move[2];
        pieces[dest].setImage(imageCache.computeIfAbsent(5 * color, _ ->
                new Image(new File("src/piece_images/" + 5 + color + ".png").toURI().toString())));
        pieces[dest + 8 * color].setImage(imageCache.computeIfAbsent(2 * color, _ ->
                new Image(new File("src/piece_images/" + 2 * color + ".png").toURI().toString())));
        pieces[dest + 16 * color].setImage(imageCache.computeIfAbsent(4 * color, _ ->
                new Image(new File("src/piece_images/" + 4 * color + ".png").toURI().toString())));
        pieces[dest + 24 * color].setImage(imageCache.computeIfAbsent(3 * color, _ ->
                new Image(new File("src/piece_images/" + 3 * color + ".png").toURI().toString())));
        pieces[move[1]].setVisible(false);
        pieces[dest].setVisible(true);
        pieces[dest + 8 * color].setVisible(true);
        pieces[dest + 16 * color].setVisible(true);
        pieces[dest + 24 * color].setVisible(true);
        promotionChoice = true;
        promotionMove = move;
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

    private static double probabilityConclusive(double currentLLR, int trialsRemaining, int wins,
                                                int draws, int losses) {
        wins++;
        draws++;
        losses++;
        int games = wins + draws + losses;
        if (games < 2) return 1.0;

        double score = (wins + 0.5 * draws) / games;
        double eloEstimate = -400.0 * log10(1.0 / score - 1.0);
        double variance = (wins * pow(1 - score, 2) + draws * pow(0.5 - score, 2) +
                losses * pow(score, 2)) / games;
        double scoreSE = sqrt(variance / games);

        double dElo_dScore = 400.0 / (log(10) * score * (1 - score));
        double eloSE = Math.abs(dElo_dScore) * scoreSE;
        double extremeElo = eloEstimate + (currentLLR > 0 ? 3 : -3) * eloSE;
        double drawRate = (double) draws / games;

        double[] q0 = pentaProbabilities(0, drawRate);
        double[] q1 = pentaProbabilities(7, drawRate);

        double[] qOpt = pentaProbabilities(extremeElo, drawRate);

        double delta = 0;
        double secondMoment = 0;

        for (int i = 0; i < 5; i++) {
            double contrib = log(q1[i] / q0[i]);
            delta += qOpt[i] * contrib;
            secondMoment += qOpt[i] * contrib * contrib;
        }

        double sigma = sqrt(max(1e-12, secondMoment - delta * delta));

        double upper = log((1.0 - 0.05) / 0.05);
        double lower = log(0.05 / (1.0 - 0.05));
        double target = currentLLR > 0 ? upper : lower;

        double meanFinal = currentLLR + trialsRemaining * delta;
        double sdFinal = sigma * sqrt(trialsRemaining);
        double z = currentLLR > 0 ? (target - meanFinal) / sdFinal : (meanFinal - target) / sdFinal;

        return 1 - normalCDF(z);
    }

    /**
     * Returns the probability that the SPRT will be conclusive by the end of the simulation given
     * the current LLR and the number of remaining games.
     *
     * @param llr Log-Likelihood Ratio from the SPRT
     * @return The probability that the SPRT will be conclusive by the end of the simulation given
     * the current LLR and the number of remaining games.
     */
    private static double probabilityConclusive(double llr) {
        return probabilityConclusive(llr, 1000 - testIdx, wins, draws, losses);
    }

    private static boolean stop(double llr) {
        double upper = Math.log((1.0 - 0.05) / 0.05);
        double lower = Math.log(0.05 / (1.0 - 0.05));
        return llr > upper || llr < lower;
    }

    private static double expectedScore(double elo) {
        return 1 / (1 + Math.pow(10, -elo / 400));
    }

    private static double[] pentaProbabilities(double elo, double drawRate) {
        double[] q = new double[5];
        double winRate = expectedScore(elo) - drawRate * 0.5;
        double lossRate = 1 - winRate - drawRate;

        q[0] = max(1e-12, lossRate * lossRate);
        q[1] = max(1e-12, 2 * lossRate * drawRate);
        q[2] = max(1e-12, drawRate * drawRate + 2 * winRate * lossRate);
        q[3] = max(1e-12, 2 * drawRate * winRate);
        q[4] = max(1e-12, winRate * winRate);

        return q;
    }

    private static double SPRT() {
        int p0 = penta[0];
        int p1 = penta[1];
        int p2 = penta[2];
        int p3 = penta[3];
        int p4 = penta[4];
        int n = p0 + p1 + p2 + p3 + p4;
        if (n < 2) return 0;

        double elo0 = 0;
        double elo1 = 7;

        double drawRate = (double) draws / (2 * n);

        double[] q0 = pentaProbabilities(elo0, drawRate);
        double[] q1 = pentaProbabilities(elo1, drawRate);

        double llr = 0;
        for (int i = 0; i < 5; i++) llr += penta[i] * log(q1[i] / q0[i]);

        return llr;
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
                if (gameState.getWinner() == 2 * (testIdx % 2) - 1) wins++;
                else if (gameState.getWinner() != 0) losses++;
                else draws++;
                if ((draws + losses + wins) % 2 == 1)
                    lastScore = (gameState.getWinner() == 2 * (testIdx % 2) - 1) ? 1 :
                            gameState.getWinner() == 0 ? 0.5 : 0;
                else if (draws + losses + wins > 0) {
                    lastScore += (gameState.getWinner() == 2 * (testIdx % 2) - 1) ? 1 :
                            gameState.getWinner() == 0 ? 0.5 : 0;
                    penta[(int) (lastScore * 2)]++;
                }
                testIdx++;
                pValue = getPValue(losses, draws, wins);
                llr = SPRT();
                pConclusive = probabilityConclusive(llr);
                IO.println("Curr Wins: " + losses + " Test Wins: " + wins +
                        " Draws: " + draws + " P-Value: " + format("%.4f", pValue) +
                        " LLR: " + format("%.3f", llr) +
                        " Curr Depth: " + format("%.2f", trimmedMean(currDepths)) +
                        " Test Depth: " + format("%.2f", trimmedMean(testDepths)));
                Platform.runLater(this::updateInfoTextDeepTest);
                if ((testIdx == 20 && pValue < .01) || testIdx >= 1000 || pValue < .001
                        || pConclusive < .01 || stop(llr)) {
                    if (testIdx == 20 && pValue < .01)
                        IO.println("Automatically stopped at p = " + format("%.4f", pValue));
                    if (stop(llr) && testIdx < 1000)
                        IO.println("Automatically stopped at LLR = " + format("%.3f", llr));
                    if (pValue < .001)
                        IO.println("Automatically stopped at p = " + format("%.4f", pValue));
                    if (pConclusive < .01)
                        IO.println("Automatically stopped at pConclusive = " +
                                format("%.4f", pConclusive));
                    deepTest = false;
                    testIdx = 0;
                    whitePlayerHuman = true;
                    blackPlayerHuman = true;
                    currBot.setPrinting(true);
                    testBot.setPrinting(true);
                    Platform.runLater(() -> {
                        btnDeepTest.setText("Deep Test");
                        btnReset.setText("Reset Position");
                        SoundHandler.playSound("game-end");
                    });
                    shown = true;
                    IO.println("Curr Bot: " + currBot);
                    printHistogram(currDepths);
                    IO.println("Test Bot: " + testBot);
                    printHistogram(testDepths);
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
            makeMove((deepTest && (gameState.getColor() == 2 * (testIdx % 2) - 1) ?
                    testBot.getMove(gameState, allottedTime) :
//                    testBot.iterativeDeepening(gameState, 2) :
                    currBot.getMove(gameState, allottedTime)));
//                    currBot.iterativeDeepening(gameState, 2)));
            if (gameState.getColor() == 2 * (testIdx % 2) - 1)
                currDepths[min(currBot.getLastDepth(), 63)]++;
            else testDepths[min(testBot.getLastDepth(), 63)]++;
            Platform.runLater(this::updateInfoText);
            gameState.computeMoves();
            if (deepTest || (gameState.isWhiteMove() ? !whitePlayerHuman : !blackPlayerHuman))
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
        darkRect.setX(max(0, (width - height) / 2));
        darkRect.setY(max(0, (height - width) / 2));
        darkRect.setWidth(length * 8);
        darkRect.setHeight(length * 8);
        div.positionElements(scene);
        if ((width - height) / 2 < 120)
            infoText.setVisible(false);
        else {
            boolean wasVisible = infoText.isVisible();
            infoText.setVisible(true);
            if (!wasVisible) scene.getRoot().requestFocus();
            infoText.setTranslateX((width - height) / 2 + height + 10);
            infoText.setWrappingWidth(clamp((width - height) / 2 - 20, 100,
                    clamp(150 * height / 480, 120, 220)));
            infoText.setFont(new Font(clamp(height / 480 * 12, 12, 20)));
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
            if (!promotionChoice) piece.setVisible(false);
            return;
        }
        if (!promotionChoice) piece.setVisible(true);
        if (!promotionChoice) piece.setImage(imageCache.computeIfAbsent(pieceType, c ->
                new Image(new File("src" + "/piece_images/" + c + ".png").toURI().toString())));
        if (i == selectedSquare && dragging) {
            piece.setX(mousePose[0] - squareWidth / 2);
            piece.setY(mousePose[1] - squareWidth / 2);
            piece.toFront();
            darkRect.toFront();
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
        if (deepTest || (gameState.isWhiteMove() ? !whitePlayerHuman : !blackPlayerHuman))
            return false;
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
