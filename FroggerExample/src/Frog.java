import java.awt.*;
import java.awt.image.BufferedImage;

class Frog {
    int x, y;
    int w = GamePanel.TILE - 8;
    int h = GamePanel.TILE - 8;
    boolean alive = true;

    Frog(int x, int y) { this.x = x; this.y = y; }

    void nudge(int dx, int dy) {
        x += dx; y += dy;
        clampToBoard(GamePanel.WIDTH);
    }

    void clampToBoard(int boardW) {
        x = Math.max(0, Math.min(boardW - w, x));
        y = Math.max(GamePanel.TILE, Math.min((GamePanel.ROWS-1)*GamePanel.TILE + 4, y));
    }

    void update() {}

    Rectangle bounds() { return new Rectangle(x, y, w, h); }
     
    // entity draw method
    void draw(Graphics2D g) {
        BufferedImage spr = Assets.frog();
        if (spr != null) {
            g.drawImage(spr, x + 4, y + 4, w - 8, h - 8, null);
        } else {
            int px = x + 4, py = y + 4;
            g.setColor(new Color(88,176,88)); g.fillRect(px, py, w-8, h-8);
            g.setColor(Color.BLACK); g.drawRect(px, py, w-8, h-8);
        }
    }
}
