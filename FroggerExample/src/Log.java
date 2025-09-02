import java.awt.*;

class Log {
    double x;
    final int y, w, h;
    final double speed;

    Log(int x, int y, int w, int h, double speed) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.speed = speed;
    }

    void update() { x += speed; }

    Rectangle bounds() { return new Rectangle((int)x, y, w, h); }

    void draw(Graphics2D g) {
        g.setColor(new Color(139, 90, 43));
        g.fillRoundRect((int)x, y, w, h, 8, 8);
        g.setColor(new Color(100, 60, 30));
        g.drawRoundRect((int)x, y, w, h, 8, 8);
        // wood grain lines
        g.drawLine((int)x + 10, y + h/3, (int)x + w - 10, y + h/3);
        g.drawLine((int)x + 10, y + 2*h/3, (int)x + w - 10, y + 2*h/3);
    }
}
