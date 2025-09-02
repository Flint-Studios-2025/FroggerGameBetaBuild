import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

class GamePanel extends JPanel implements ActionListener, KeyListener {
    static final int TILE = 40;
    static final int COLS = 16;
    static final int ROWS = 18;
    static final int WIDTH = COLS * TILE;
    static final int HEIGHT = ROWS * TILE;

    private final javax.swing.Timer timer;
    private final Frog frog;
    private final java.util.List<Car> cars = new ArrayList<>();
    private final java.util.List<Truck> trucks = new ArrayList<>();
    private final java.util.List<Log> logs = new ArrayList<>();
    private final java.util.List<Bird> birds = new ArrayList<>();
    private final java.util.List<Snake> snakes = new ArrayList<>();
    private final java.util.List<Lane> roadLanes = new ArrayList<>();
    private final java.util.List<Lane> riverLanes = new ArrayList<>();
    private final java.util.List<Lane> critterLanes = new ArrayList<>();
    private final Random rng = new Random();

    // ===== Tuning knobs =====
    private double trafficScale = 1.7;        // bigger = fewer cars/trucks
    private double logDensityScale = 1.4;     // bigger = fewer logs
    private double critterDensityScale = 1.6; // bigger = fewer birds/snakes

    // Spacing (no-overlap)
    private final int VEHICLE_MIN_GAP = TILE;   // vehicles gap
    private final int CRITTER_MIN_GAP = TILE/2; // birds/snakes gap

    private int level = 1;
    private int lives = 3;
    private int ticks = 0;
    private boolean paused = false;
    private boolean showHelp = true;

    private int score = 0;
    private int bestRowY;

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setBackground(new Color(15, 20, 35));
        addKeyListener(this);
        frog = new Frog(WIDTH/2 - TILE/2, (ROWS-1)*TILE + 4);
        bestRowY = frog.y;
        timer = new javax.swing.Timer(16, this); // ~60fps
        timer.start();
        setupLevel();
    }

    void start() { if (!timer.isRunning()) timer.start(); }

    @Override public void addNotify() { super.addNotify(); requestFocusInWindow(); }

    private void setupLevel() {
        cars.clear(); trucks.clear(); logs.clear(); birds.clear(); snakes.clear();
        roadLanes.clear(); riverLanes.clear(); critterLanes.clear();

        // River lanes: exactly 3 rows (board rows 2,3,4)
        int[] riverRows = {4, 3, 2};
        for (int i = 0; i < riverRows.length; i++) {
            int row = riverRows[i];
            boolean leftToRight = (i % 2 == 0);
            double baseSpeed = 1.25 + 0.2 * i + (level-1) * 0.15;
            int spawnEvery = Math.max(48 - level*2 - i*2, 18);
            spawnEvery = (int)Math.round(spawnEvery * logDensityScale);
            riverLanes.add(new Lane(row*TILE, leftToRight ? baseSpeed : -baseSpeed, spawnEvery));
        }

        // Road lanes: rows 6..11
        int[] roadRows = {11,10,9,8,7,6};
        for (int i = 0; i < roadRows.length; i++) {
            int row = roadRows[i];
            boolean leftToRight = (i % 2 == 0);
            double baseSpeed = 1.5 + 0.25 * i + (level-1) * 0.25;
            int spawnEvery = Math.max(35 - level*2 - i*2, 15);
            spawnEvery = (int)Math.round(spawnEvery * trafficScale);
            roadLanes.add(new Lane(row*TILE, leftToRight ? baseSpeed : -baseSpeed, spawnEvery));
        }

        // Bottom critter lanes: rows 14..16 (just above start)
        int[] critterRows = {16, 15, 14};
        for (int i = 0; i < critterRows.length; i++) {
            int row = critterRows[i];
            boolean leftToRight = (i % 2 == 0);
            double baseSpeed = 1.4 + 0.2 * i + (level-1) * 0.15; // moderate speed
            int spawnEvery = Math.max(40 - level*2 - i, 16);
            spawnEvery = (int)Math.round(spawnEvery * critterDensityScale);
            critterLanes.add(new Lane(row*TILE, leftToRight ? baseSpeed : -baseSpeed, spawnEvery));
        }

        // Seed initial road traffic with spacing
        for (Lane lane : roadLanes) {
            int toPlace = 1 + rng.nextInt(2);
            int attempts = 0;
            while (toPlace > 0 && attempts < 12) {
                attempts++;
                boolean spawnTruck = rng.nextDouble() < 0.45;
                if (spawnTruck) {
                    int w = TILE * (3 + rng.nextInt(2));
                    int h = TILE - 8;
                    int y = lane.y + 4; int x = rng.nextInt(WIDTH);
                    if (laneHasSpaceFor(x, y, w, h, lane, VEHICLE_MIN_GAP)) { trucks.add(new Truck(x, y, w, h, lane.speed * 0.85)); toPlace--; }
                } else {
                    int w = TILE * (2 + rng.nextInt(2));
                    int h = TILE - 10;
                    int y = lane.y + 5; int x = rng.nextInt(WIDTH);
                    if (laneHasSpaceFor(x, y, w, h, lane, VEHICLE_MIN_GAP)) { cars.add(new Car(x, y, w, h, lane.speed)); toPlace--; }
                }
            }
        }

        // Seed logs with mild spacing
        for (Lane lane : riverLanes) {
            int toPlace = 1 + (rng.nextBoolean() ? 1 : 0);
            int attempts = 0;
            while (toPlace > 0 && attempts < 10) {
                attempts++;
                int w = TILE * (3 + rng.nextInt(2));
                int h = TILE - 12; int y = lane.y + 6; int x = rng.nextInt(WIDTH);
                if (logLaneHasSpaceFor(x, y, w, h, lane, TILE/2)) { logs.add(new Log(x, y, w, h, lane.speed)); toPlace--; }
            }
        }

        // Seed critters with spacing
        for (Lane lane : critterLanes) {
            int toPlace = 1 + rng.nextInt(2);
            int attempts = 0;
            while (toPlace > 0 && attempts < 12) {
                attempts++;
                boolean bird = rng.nextBoolean();
                if (bird) {
                    int w = TILE - 8; int h = TILE - 14; int y = lane.y + 7; int x = rng.nextInt(WIDTH);
                    if (critterLaneHasSpaceFor(x, y, w, h, lane, CRITTER_MIN_GAP)) { birds.add(new Bird(x, y, w, h, lane.speed * 1.1)); toPlace--; }
                } else {
                    int w = (int)(TILE * 1.6); int h = TILE - 12; int y = lane.y + 6; int x = rng.nextInt(WIDTH);
                    if (critterLaneHasSpaceFor(x, y, w, h, lane, CRITTER_MIN_GAP)) { snakes.add(new Snake(x, y, w, h, lane.speed * 0.9)); toPlace--; }
                }
            }
        }

        // IMPORTANT: place the frog LAST so nothing overrides it
        resetFrog(false);
    }

    private void resetFrog(boolean keepRowBonus) {
        if (!keepRowBonus) bestRowY = (ROWS-1)*TILE + 4;
        frog.x = WIDTH / 2 - TILE / 2;
        frog.y = (ROWS - 1) * TILE + 4;
        frog.alive = true;
        repaint(); // force immediate redraw so reset is visible
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (paused) { repaint(); return; }
        ticks++;

        // ===== Spawning =====
        for (Lane lane : roadLanes) {
            if (ticks % lane.spawnEvery == 0) {
                double roll = rng.nextDouble();
                if (roll < 0.25) {
                    int w = TILE * (3 + rng.nextInt(2)); int h = TILE - 8; int y = lane.y + 4;
                    int x = lane.speed > 0 ? -w - 10 : WIDTH + 10;
                    if (laneHasSpaceFor(x, y, w, h, lane, VEHICLE_MIN_GAP)) trucks.add(new Truck(x, y, w, h, lane.speed * 0.85));
                } else if (roll < 0.65) {
                    int w = TILE * (2 + rng.nextInt(2)); int h = TILE - 10; int y = lane.y + 5;
                    int x = lane.speed > 0 ? -w - 10 : WIDTH + 10;
                    if (laneHasSpaceFor(x, y, w, h, lane, VEHICLE_MIN_GAP)) cars.add(new Car(x, y, w, h, lane.speed));
                }
            }
        }
        for (Lane lane : riverLanes) {
            if (ticks % lane.spawnEvery == 0 && rng.nextDouble() < 0.75) {
                int w = TILE * (3 + rng.nextInt(2)); int h = TILE - 12; int y = lane.y + 6;
                int x = lane.speed > 0 ? -w - 10 : WIDTH + 10;
                if (logLaneHasSpaceFor(x, y, w, h, lane, TILE/3)) logs.add(new Log(x, y, w, h, lane.speed));
            }
        }
        for (Lane lane : critterLanes) {
            if (ticks % lane.spawnEvery == 0 && rng.nextDouble() < 0.65) {
                boolean bird = rng.nextBoolean();
                if (bird) {
                    int w = TILE - 8; int h = TILE - 14; int y = lane.y + 7;
                    int x = lane.speed > 0 ? -w - 10 : WIDTH + 10;
                    if (critterLaneHasSpaceFor(x, y, w, h, lane, CRITTER_MIN_GAP)) birds.add(new Bird(x, y, w, h, lane.speed * 1.1));
                } else {
                    int w = (int)(TILE * 1.6); int h = TILE - 12; int y = lane.y + 6;
                    int x = lane.speed > 0 ? -w - 10 : WIDTH + 10;
                    if (critterLaneHasSpaceFor(x, y, w, h, lane, CRITTER_MIN_GAP)) snakes.add(new Snake(x, y, w, h, lane.speed * 0.9));
                }
            }
        }

        // ===== Updates =====
        cars.forEach(Car::update);
        trucks.forEach(Truck::update);
        logs.forEach(Log::update);
        birds.forEach(Bird::update);
        snakes.forEach(Snake::update);

        // Keep gaps so nothing overlaps after movement
        resolveVehicleGaps();
        resolveCritterGaps();

        // Cleanup offscreen
        cars.removeIf(c -> c.x < -c.w - 60 || c.x > WIDTH + 60);
        trucks.removeIf(t -> t.x < -t.w - 60 || t.x > WIDTH + 60);
        logs.removeIf(l -> l.x < -l.w - 60 || l.x > WIDTH + 60);
        birds.removeIf(b -> b.x < -b.w - 60 || b.x > WIDTH + 60);
        snakes.removeIf(s -> s.x < -s.w - 60 || s.x > WIDTH + 60);

        // Frog movement & scoring
        frog.update();
        if (frog.y < bestRowY) { int rowsUp = (bestRowY - frog.y) / TILE; if (rowsUp > 0) { score += rowsUp * 10; bestRowY = frog.y; } }

        // Collisions with hazards
        Rectangle fr = frog.bounds();
        for (Car c : cars)   if (c.bounds().intersects(fr)) { die(); repaint(); return; }
        for (Truck t : trucks) if (t.bounds().intersects(fr)) { die(); repaint(); return; }
        for (Bird b : birds) if (b.bounds().intersects(fr)) { die(); repaint(); return; }
        for (Snake s : snakes) if (s.bounds().intersects(fr)) { die(); repaint(); return; }

        // River (rows 2..4) requires a log
        boolean inRiver = frog.y >= 2*TILE && frog.y < 5*TILE;
        if (inRiver) {
            boolean onLog = false; double carry = 0;
            for (Log l : logs) if (l.bounds().intersects(fr)) { onLog = true; carry = l.speed; break; }
            if (!onLog) { die(); repaint(); return; }
            frog.x += carry; frog.clampToBoard(WIDTH);
        }

        // Goal reached
        if (frog.y <= TILE) { score += 100; level++; setupLevel(); /* resetFrog(false) happens inside setupLevel() */ }

        repaint();
    }

    private void die() {
        if (!frog.alive) return;
        frog.alive = false;
        lives--; score = Math.max(0, score - 25);
        if (lives <= 0) { level = 1; lives = 3; score = 0; }
        // Rebuild level; resetFrog(false) is called at the END of setupLevel()
        setupLevel();
    }

    // ===== Spacing utilities =====
    private boolean laneHasSpaceFor(int x, int y, int w, int h, Lane lane, int gap) {
        Rectangle cand = new Rectangle(x, y, w, h);
        for (Car c : cars) if (Math.abs(c.y - lane.y) < TILE/2 && expanded(c.bounds(), gap).intersects(cand)) return false;
        for (Truck t : trucks) if (Math.abs(t.y - lane.y) < TILE/2 && expanded(t.bounds(), gap).intersects(cand)) return false;
        return true;
    }
    private boolean logLaneHasSpaceFor(int x, int y, int w, int h, Lane lane, int gap) {
        Rectangle cand = new Rectangle(x, y, w, h);
        for (Log l : logs) if (Math.abs(l.y - lane.y) < TILE/2 && expanded(l.bounds(), gap).intersects(cand)) return false;
        return true;
    }
    private boolean critterLaneHasSpaceFor(int x, int y, int w, int h, Lane lane, int gap) {
        Rectangle cand = new Rectangle(x, y, w, h);
        for (Bird b : birds) if (Math.abs(b.y - lane.y) < TILE/2 && expanded(b.bounds(), gap).intersects(cand)) return false;
        for (Snake s : snakes) if (Math.abs(s.y - lane.y) < TILE/2 && expanded(s.bounds(), gap).intersects(cand)) return false;
        return true;
    }
    private Rectangle expanded(Rectangle r, int gap) { return new Rectangle(r.x - gap, r.y, r.width + 2*gap, r.height); }

    private void resolveVehicleGaps() {
        for (Lane lane : roadLanes) {
            java.util.List<RectangleCarrier> list = new ArrayList<>();
            for (Car c : cars) if (Math.abs(c.y - lane.y) < TILE/2) list.add(new RectangleCarrier(c));
            for (Truck t : trucks) if (Math.abs(t.y - lane.y) < TILE/2) list.add(new RectangleCarrier(t));
            resolveLaneGaps(list, lane.speed > 0, VEHICLE_MIN_GAP);
        }
    }
    private void resolveCritterGaps() {
        for (Lane lane : critterLanes) {
            java.util.List<RectangleCarrier> list = new ArrayList<>();
            for (Bird b : birds) if (Math.abs(b.y - lane.y) < TILE/2) list.add(new RectangleCarrier(b));
            for (Snake s : snakes) if (Math.abs(s.y - lane.y) < TILE/2) list.add(new RectangleCarrier(s));
            resolveLaneGaps(list, lane.speed > 0, CRITTER_MIN_GAP);
        }
    }
    private void resolveLaneGaps(java.util.List<RectangleCarrier> list, boolean right, int gap) {
        if (list.size() < 2) return;
        list.sort((a,b) -> right ? Double.compare(a.getX(), b.getX()) : Double.compare(b.getX(), a.getX()));
        RectangleCarrier prev = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            RectangleCarrier curr = list.get(i);
            Rectangle pr = prev.bounds();
            Rectangle cr = curr.bounds();
            if (right) {
                int minX = pr.x + pr.width + gap;
                if (cr.x < minX) curr.addX(minX - cr.x);
            } else {
                int maxX = pr.x - gap - cr.width;
                if (cr.x > maxX) curr.addX(-(cr.x - maxX));
            }
            prev = curr;
        }
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // HUD
        g.setColor(new Color(8, 12, 22)); g.fillRect(0, 0, WIDTH, TILE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 18f)); g.setColor(Color.WHITE);
        g.drawString("Level: " + level + "   Lives: " + lives + "   Score: " + score + (paused ? "   [PAUSED]" : ""), 10, 26);

        // Goal band (top)
        g.setColor(new Color(30, 100, 50)); g.fillRect(0, TILE, WIDTH, TILE);

        // River: rows 2..4
        g.setColor(new Color(30, 60, 120)); g.fillRect(0, 2*TILE, WIDTH, 3*TILE);

        // Middle road: rows 6..11
        g.setColor(new Color(35, 35, 35)); g.fillRect(0, 6*TILE, WIDTH, 6*TILE);

        // Bottom critter lanes background (grass) rows 14..16
        g.setColor(new Color(35, 120, 55)); g.fillRect(0, 14*TILE, WIDTH, 3*TILE);

        // Start band (bottom row)
        g.setColor(new Color(30, 100, 50)); g.fillRect(0, (ROWS-1)*TILE, WIDTH, TILE);

        // Road markings
        g.setStroke(new BasicStroke(2f)); g.setColor(new Color(220, 200, 70));
        for (Lane lane : roadLanes) for (int x = 0; x < WIDTH; x += 60) g.drawLine(x, lane.y + TILE/2, x + 30, lane.y + TILE/2);

        // Draw order: logs, trucks, cars, birds, snakes, frog
        logs.forEach(l -> l.draw(g));
        trucks.forEach(t -> t.draw(g));
        cars.forEach(c -> c.draw(g));
        birds.forEach(b -> b.draw(g));
        snakes.forEach(s -> s.draw(g));
        frog.draw(g);

        if (showHelp) {
            String[] help = {
                "Controls:", "Arrow keys = move", "P = pause, H = toggle help",
                "River: 3 lanes (rows 2–4)", "Bottom: birds & snakes (rows 14–16)",
                "No overlap: vehicles & critters keep gaps",
                "Reach top (+100), +10 per new row"
            };
            int w = 540, h = 196; int x = WIDTH/2 - w/2, y = HEIGHT/2 - h/2;
            g.setColor(new Color(0,0,0,170)); g.fillRoundRect(x,y,w,h,16,16);
            g.setColor(Color.WHITE); g.drawRoundRect(x,y,w,h,16,16);
            int yy = y + 28; g.setFont(g.getFont().deriveFont(Font.PLAIN,16f));
            for(String line:help){ g.drawString(line,x+16,yy); yy+=22; }
        }
    }

    // Input
    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> frog.nudge(-TILE, 0);
            case KeyEvent.VK_RIGHT -> frog.nudge(TILE, 0);
            case KeyEvent.VK_UP -> frog.nudge(0, -TILE);
            case KeyEvent.VK_DOWN -> frog.nudge(0, TILE);
            case KeyEvent.VK_P -> paused = !paused;
            case KeyEvent.VK_H -> showHelp = !showHelp;
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}

// Helper wrapper to move heterogeneous objects uniformly
class RectangleCarrier {
    private final Object o;
    RectangleCarrier(Object o){ this.o = o; }
    Rectangle bounds(){
        if (o instanceof Car c) return c.bounds();
        if (o instanceof Truck t) return t.bounds();
        if (o instanceof Bird b) return b.bounds();
        if (o instanceof Snake s) return s.bounds();
        return new Rectangle();
    }
    double getX(){
        if (o instanceof Car c) return c.x;
        if (o instanceof Truck t) return t.x;
        if (o instanceof Bird b) return b.x;
        if (o instanceof Snake s) return s.x;
        return 0;
    }
    void addX(double dx){
        if (o instanceof Car c) c.x += dx;
        else if (o instanceof Truck t) t.x += dx;
        else if (o instanceof Bird b) b.x += dx;
        else if (o instanceof Snake s) s.x += dx;
    }
}