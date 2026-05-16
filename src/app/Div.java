package app;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Font;

import static java.lang.Math.clamp;
import static java.lang.Math.min;

public class Div {
    private final Control[] elements;
    private int numAdded = 0;

    public Div(int numButtons) {
        elements = new Control[numButtons];
    }

    public void add(Control ele) {
        elements[numAdded] = ele;
        numAdded++;
    }

    public Control[] getElements() {
        return elements;
    }

    public void positionElements(Scene scene) {
        double width = scene.getWidth();
        double height = scene.getHeight();
        double eleSize = clamp(min(((width - height) / 2 - 20) / 100, height / 480), 1, 1.5);
        if ((width - height) / 2 < 120) {
            for (Control ele : elements) ele.setVisible(false);
        } else {
            double x = (width - height) / 2 - 10 - 100 * eleSize;
            double y = height / 2 + 10 - (elements.length) * 25 * eleSize;
            for (Control ele : elements) {
                ele.setVisible(true);
                ele.setTranslateX(x);
                ele.setTranslateY(y);
                ele.setPrefWidth(100 * eleSize);
                ele.setPrefHeight(30 * eleSize);
                if (ele instanceof Labeled labeled) labeled.setFont(new Font(12 * eleSize));
                else if (ele instanceof TextInputControl input)
                    input.setFont(new Font(14 * eleSize));
                y += 50 * eleSize;
            }
        }
    }
}
