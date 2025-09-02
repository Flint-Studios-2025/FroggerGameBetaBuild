import java.awt.*;

class Snake {
    double x;
    final int y, w, h;
    final double speed;

    Snake(int x, int y, int w, int h, double speed) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.speed = speed;
    }

    void update() { x += speed; }

    Rectangle bounds() { return new Rectangle((int)x, y, w, h); }

    void draw(Graphics2D g) {
        int xi = (int)x;
        g.setColor(new Color(40, 160, 70));
        g.fillRoundRect(xi, y, w, h, 10, 10);
        g.setColor(new Color(20, 120, 50));
        for (int i = 6; i < w - 6; i += 10) {
            g.fillOval(xi + i, y + h/3, 6, 6);
        }
        g.setColor(new Color(240, 170, 50));
        g.fillRect(xi + w - 6, y + h/2 - 2, 6, 4); // tongue
    }
}