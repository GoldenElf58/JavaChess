import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class Arrow extends Polygon {
    private static final double defaultArrowHeadSize = 5.0;
    private final double startX;
    private final double startY;
    private final double endX;
    private final double endY;
    private boolean drawTail = true;
    double x1;
    double y1;
    double x2;
    double y2;
    double x3;
    double y3;

    public Arrow(double startX, double startY, double endX, double endY, double arrowHeadSize) {
        super();
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
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
        getPoints().addAll(x3, y3);
        getPoints().addAll(x1, y1);
        getPoints().addAll(endX, endY);
        getPoints().addAll(x2, y2);
        getPoints().addAll(x3, y3);
    }

    public Arrow(double startX, double startY, double endX, double endY) {
        this(startX, startY, endX, endY, defaultArrowHeadSize);
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
}