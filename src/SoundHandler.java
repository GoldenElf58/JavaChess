import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SoundHandler {

    private static final Map<String, Media> soundCache = new HashMap<>();
    private static boolean soundsLoaded = false;
    private static final String[] files = {"move-check", "move-opponent", "move-self", "capture",
            "castle", "promote", "game-end", "illegal", "click"};

    public static void loadSounds() {
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
}
