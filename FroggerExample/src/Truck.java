import java.awt.*;

/**
 * Bigger, slower vehicle.
 */
class Truck {
    double x;
    final int y, w, h;
    final double speed;

    Truck(int x, int y, int w, int h, double speed) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.speed = speed;
    }

    void update() { x += speed; }

    Rectangle bounds() { return new Rectangle((int)x, y, w, h); }

    void draw(Graphics2D g) {
        // body
        g.setColor(new Color(70, 120, 200));
        g.fillRoundRect((int)x, y, w, h, 10, 10);
        // cab
        g.setColor(new Color(50, 90, 160));
        g.fillRoundRect((int)x + w/10, y + h/6, w/4, h - h/3, 10, 10);
        // bumper/shadow
        g.setColor(Color.DARK_GRAY);
        g.fillRect((int)x + 8, y + h - 8, w - 16, 6);
    }
}