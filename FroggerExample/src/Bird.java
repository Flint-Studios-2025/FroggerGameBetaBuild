import java.awt.*;

class Bird {
    double x;
    final int y, w, h;
    final double speed;

    Bird(int x, int y, int w, int h, double speed) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.speed = speed;
    }

    void update() { x += speed; }

    Rectangle bounds() { return new Rectangle((int)x, y, w, h); }

    void draw(Graphics2D g) {
        int xi = (int)x;
        g.setColor(new Color(230, 230, 230));
        g.fillOval(xi, y + h/4, w/2, h/2); // body
        g.fillOval(xi + w/3, y + h/3, w/2, h/3); // wings
        g.setColor(Color.DARK_GRAY);
        g.fillOval(xi + w/4, y + h/2 - 2, 4, 4); // eye
        g.setColor(new Color(240, 170, 50));
        g.fillPolygon(new int[]{xi + w - 4, xi + w + 4, xi + w - 4}, new int[]{y + h/2 - 2, y + h/2, y + h/2 + 2}, 3); // beak
    }
}
