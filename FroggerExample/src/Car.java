import java.awt.*;

class Car {
    double x;
    final int y, w, h;
    final double speed;

    Car(int x, int y, int w, int h, double speed) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.speed = speed;
    }

    void update() { x += speed; }

    Rectangle bounds() { return new Rectangle((int)x, y, w, h); }

    void draw(Graphics2D g) {
        g.setColor(new Color(180, 60, 60));
        g.fillRoundRect((int)x, y, w, h, 8, 8);
        g.setColor(Color.DARK_GRAY);
        g.fillRect((int)x + 8, y + h - 8, w - 16, 6);
    }
}

