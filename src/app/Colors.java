package app;

import javafx.scene.paint.Color;

public class Colors {

    public static final Color DARK_COLOR = Color.rgb(181, 136, 99);
    public static final Color LIGHT_COLOR = Color.rgb(240, 217, 181);

    public static final Color DARK_SELECTED_COLOR = Color.rgb(100, 110, 64);
    public static final Color LIGHT_SELECTED_COLOR = Color.rgb(130, 151, 105);

    public static final Color DARK_HOVER_COLOR = Color.rgb(132, 121, 78);
    public static final Color LIGHT_HOVER_COLOR = Color.rgb(174, 177, 135);

    public static final Color DARK_MOVE_COLOR = Color.rgb(170, 162, 58);
    public static final Color LIGHT_MOVE_COLOR = Color.rgb(205, 210, 106);

    public static final Color PENCIL_COLOR = Color.rgb(21, 120, 27);

    public static Color getSquareBaseColor(boolean light) {
        return light ? LIGHT_COLOR : DARK_COLOR;
    }

    public static Color getSquareSelectedColor(boolean light) {
        return light ? LIGHT_SELECTED_COLOR : DARK_SELECTED_COLOR;
    }

    public static Color getSquareHoverColor(boolean light) {
        return light ? LIGHT_HOVER_COLOR : DARK_HOVER_COLOR;
    }

    public static Color getSquareMoveColor(boolean light) {
        return light ? LIGHT_MOVE_COLOR : DARK_MOVE_COLOR;
    }
}
