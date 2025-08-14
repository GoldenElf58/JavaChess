import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class Arrow extends Polygon {
    private static final double defaultArrowHeadSize = 5.0;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private boolean drawTail = true;
    private double arrowHeadSize = defaultArrowHeadSize;
    private double x1;
    private double y1;
    private double x2;
    private double y2;
    private double x3;
    private double y3;

    public Arrow(double startX, double startY, double endX, double endY, double arrowHeadSize) {
        super();
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        setArrowHeadSize(arrowHeadSize);
    }

    public void setArrowHeadSize(double arrowHeadSize) {
        this.arrowHeadSize = arrowHeadSize;
        calculatePoints();
        getPoints().clear();
        addPoints();
    }

    private void calculatePoints() {
        double angle = Math.atan2((endY - startY), (endX - startX)) - Math.PI / 2.0;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        x1 = (-1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * arrowHeadSize + endX;
        y1 = (-1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * arrowHeadSize + endY;
        x2 = (1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * arrowHeadSize + endX;
        y2 = (1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * arrowHeadSize + endY;
        x3 = (x1 + x2) / 2;
        y3 = (y1 + y2) / 2;
    }

    private void addPoints() {
        if (drawTail) getPoints().addAll(startX, startY);
        getPoints().addAll(lerp(x3, endX, 0.1), lerp(y3, endY, 0.1));
        getPoints().addAll(lerp(x1, endX, 0.1), lerp(y1, endY, 0.1));
        getPoints().addAll(lerp(x3, endX, 0.9), lerp(y3, endY, 0.9));
        getPoints().addAll(lerp(x2, endX, 0.1), lerp(y2, endY, 0.1));
        getPoints().addAll(lerp(x3, endX, 0.1), lerp(y3, endY, 0.1));
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public Arrow(double startX, double startY, double endX, double endY) {
        this(startX, startY, endX, endY, defaultArrowHeadSize);
    }

    public void setStartX(double startX) {
        this.startX = startX;
        getPoints().clear();
        calculatePoints();
        addPoints();
    }

    public void setStartY(double startY) {
        this.startY = startY;
        getPoints().clear();
        calculatePoints();
        addPoints();
    }

    public void setEndX(double endX) {
        this.endX = endX;
        getPoints().clear();
        calculatePoints();
        addPoints();
    }

    public void setEndY(double endY) {
        this.endY = endY;
        getPoints().clear();
        calculatePoints();
        addPoints();
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getEndX() {
        return endX;
    }

    public double getEndY() {
        return endY;
    }

    public double getX3() {
        return x3;
    }

    public double getY3() {
        return y3;
    }

    public void setDrawTail(boolean drawTail) {
        if (this.drawTail == drawTail) return;
        this.drawTail = drawTail;
        getPoints().clear();
        addPoints();
    }

    public void setStroke(Color color) {
        super.setStroke(color);
    }

    public void setFill(Color color) {
        super.setFill(color);
    }

    public boolean equals(Arrow other) {
        return this.getStartX() == other.getStartX() && this.getStartY() == other.getStartY() &&
                this.getEndX() == other.getEndX() && this.getEndY() == other.getEndY();
    }
}