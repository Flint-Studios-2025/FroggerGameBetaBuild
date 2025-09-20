import java.awt.*;
import java.awt.image.BufferedImage;

class Snake {
    double x; final int y, w, h; final double speed;
    Snake(int x, int y, int w, int h, double speed){ this.x=x; this.y=y; this.w=w; this.h=h; this.speed=speed; }
    void update(){ x += speed; }
    Rectangle bounds(){ return new Rectangle((int)x, y, w, h); }

    void draw(Graphics2D g){
        BufferedImage spr = Assets.snake();
        int xi = (int)x;
        if (spr != null) {
            if (speed > 0) { // moving right: flip (base art faces left)
                g.drawImage(spr, xi + w, y, -w, h, null);
            } else {         // moving left: draw as is
                g.drawImage(spr, xi, y, w, h, null);
            }
        } else {
            g.setColor(new Color(64,168,88));
            g.fillRect(xi,y,w,h); g.setColor(Color.BLACK); g.drawRect(xi,y,w,h);
        }
    }
}
