import java.awt.*;

class Frog {
    int x, y;         // top-left
    int vx = 0, vy = 0; // movement impulse for discrete steps
    final int size = GamePanel.TILE - 8;
    boolean alive = true;

    Frog(int x, int y) { this.x = x; this.y = y; }

    void nudge(int dx, int dy) { vx = dx; vy = dy; }

    void update() {
        if (vx != 0 || vy != 0) {
            x += vx; y += vy;
            clampToBoard(GamePanel.WIDTH);
        }
        // one tile per key press
        vx = 0; vy = 0;
    }

    void clampToBoard(int width) {
        x = Math.max(0, Math.min(x, width - size));
        y = Math.max(GamePanel.TILE, Math.min(y, (GamePanel.ROWS-1)*GamePanel.TILE + 4));
    }

    Rectangle bounds() { return new Rectangle(x, y, size, size); }

    void draw(Graphics2D g) {
        g.setColor(new Color(60, 200, 80));
        g.fillRoundRect(x, y, size, size, 10, 10);
        g.setColor(Color.WHITE);
        g.fillOval(x + size/4, y + size/6, 8, 8);
        g.fillOval(x + size - size/4 - 8, y + size/6, 8, 8);
        g.setColor(Color.BLACK);
        g.fillOval(x + size/4 + 2, y + size/6 + 2, 4, 4);
        g.fillOval(x + size - size/4 - 6, y + size/6 + 2, 4, 4);
    }
}

