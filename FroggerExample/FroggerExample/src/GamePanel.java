import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.BufferedImage;

class GamePanel extends JPanel implements ActionListener, KeyListener {
    // Board
    static final int TILE = 40;
    static final int COLS = 16;
    static final int ROWS = 18;
    static final int WIDTH = COLS * TILE;
    static final int HEIGHT = ROWS * TILE;

    // Uniform sizes
    private static final int CAR_W    = TILE * 2;
    private static final int CAR_H    = TILE - 10;
    private static final int TRUCK_W  = TILE * 3;
    private static final int TRUCK_H  = TILE - 8;
    private static final int LOG_W    = TILE * 3;
    private static final int LOG_H    = TILE - 12;
    private static final int BIRD_W   = TILE - 8;
    private static final int BIRD_H   = TILE - 14;
    private static final int SNAKE_W  = (int)(TILE * 1.6);
    private static final int SNAKE_H  = TILE - 12;

    // Update & state
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

    // Densities
    private double trafficScale = 1.6;
    private double logDensityScale = 1.4;
    private double critterDensityScale = 1.5;

    // Spacing
    private final int VEHICLE_MIN_GAP = TILE;     // desired min gap on a lane
    private final int CRITTER_MIN_GAP = TILE/2;

    private int level = 1;            // <-- Level 1 (gameplay), Level 2 (free move) for now
    private boolean freePlay = false; // true for Level 2 blank frame

    private int lives = 3;
    private int ticks = 0;
    private boolean paused = false;
    private boolean showHelp = true;

    private int score = 0;
    private int bestRowY;

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setBackground(new Color(22,18,32));
        addKeyListener(this);

        frog = new Frog(WIDTH/2 - TILE/2, (ROWS-1)*TILE + 4);
        bestRowY = frog.y;

        timer = new javax.swing.Timer(16, this); // 60fps
        timer.setCoalesce(true);
        timer.setInitialDelay(0);
        timer.start();
        setupLevel();
    }

    void start() { if (!timer.isRunning()) timer.start(); }
    @Override public void addNotify() { super.addNotify(); requestFocusInWindow(); }

    // Speed variation helper (+ or - 25% speed)
    private double vary(double base, double factor) {
        double mult = 1.0 + (rng.nextDouble()*2 - 1) * factor;
        return base * mult;
    }

    private void setupLevel() {
        cars.clear(); trucks.clear(); logs.clear(); birds.clear(); snakes.clear();
        roadLanes.clear(); riverLanes.clear(); critterLanes.clear();

        freePlay = (level >= 2); // Level 2+ is free move for now

        if (freePlay) {
            // Blank frame: keep only a simple background and the frog; no lanes/spawns/hazards. Level 2 is not complete
            resetFrog(false);
            repaint();
            return;
        }

        //  Level 1 (regular gameplay) =========

        // River lanes: rows 2-4 (alternating dir)
        int[] riverRows = {4,3,2};
        for (int i = 0; i < riverRows.length; i++) {
            int row = riverRows[i];
            boolean right = (i % 2 == 0);
            double baseSpeed = 1.25 + 0.2 * i + (level-1) * 0.15;
            int spawnEvery = Math.max(48 - level*2 - i*2, 18);
            spawnEvery = (int)Math.round(spawnEvery * logDensityScale);
            riverLanes.add(new Lane(row*TILE, right ? baseSpeed : -baseSpeed, spawnEvery));
        }

        // Road lanes (top>bottom): rows 6-11
        // Directions fixed: lanes 1,3,5 > right (+) lanes 2,4,6 > left (-)
        int[] roadRowsTopDown = {6,7,8,9,10,11};
        for (int i = 0; i < roadRowsTopDown.length; i++) {
            int row = roadRowsTopDown[i];
            boolean dirRight = ((i % 2) == 0); // lanes index 0,2,4 (1,3,5) → right
            double baseSpeed = 1.5 + 0.12 * i;
            int spawnEvery = Math.max(34 - level*2 - i*2, 14);
            spawnEvery = (int)Math.round(spawnEvery * trafficScale);
            double signedSpeed = dirRight ? baseSpeed : -baseSpeed;
            roadLanes.add(new Lane(row*TILE, signedSpeed, spawnEvery));
        }

        // Critter lanes: rows 14-16 (alternating dir)
        int[] critterRows = {16,15,14};
        for (int i = 0; i < critterRows.length; i++) {
            int row = critterRows[i];
            boolean right = (i % 2 == 0);
            double baseSpeed = 1.4 + 0.2 * i;
            int spawnEvery = Math.max(40 - level*2 - i, 16);
            spawnEvery = (int)Math.round(spawnEvery * critterDensityScale);
            critterLanes.add(new Lane(row*TILE, right ? baseSpeed : -baseSpeed, spawnEvery));
        }

        // Seed road vehicles (car, carRed, truck), directions fixed per lane, placement and speed
        for (Lane lane : roadLanes) {
            int seeded = 0, attempts = 0;
            while (seeded < 2 && attempts++ < 40) {
                int y = lane.y + 5;
                boolean truck = rng.nextDouble() < 0.33;
                int w = truck ? TRUCK_W : CAR_W;
                int h = truck ? TRUCK_H : CAR_H;
                int x = rng.nextInt(WIDTH - w);
                double sp = truck
                        ? vary(Math.copySign(Math.abs(lane.speed*0.85), lane.speed), 0.25)
                        : vary(lane.speed, 0.25);

                if (laneHasSpaceFor(lane, x, y, w, h, VEHICLE_MIN_GAP)) {
                    if (truck) trucks.add(new Truck(x, y, w, h, sp));
                    else {
                        Car.Kind kind = (rng.nextDouble() < 0.5) ? Car.Kind.RED : Car.Kind.NORMAL;
                        cars.add(new Car(x, y, w, h, sp, kind));
                    }
                    seeded++;
                }
            }
        }

        // Seed logs placement and movement speed
        for (Lane lane : riverLanes) {
            int toPlace = 2, attempts = 0;
            while (toPlace > 0 && attempts++ < 20) {
                int y = lane.y + 6, x = rng.nextInt(WIDTH-LOG_W);
                if (logLaneHasSpaceFor(lane, x, y, LOG_W, LOG_H, TILE/2)) {
                    logs.add(new Log(x, y, LOG_W, LOG_H, lane.speed));
                    toPlace--;
                }
            }
        }

        // Seed critters lanes and placement (birds, snakes)
        for (Lane lane : critterLanes) {
            int toPlace = 2, attempts = 0;
            while (toPlace > 0 && attempts++ < 20) {
                boolean bird = rng.nextBoolean();
                if (bird) {
                    int y = lane.y + 7, x = rng.nextInt(WIDTH-BIRD_W);
                    if (critterLaneHasSpaceFor(lane, x, y, BIRD_W, BIRD_H, CRITTER_MIN_GAP)) {
                        birds.add(new Bird(x, y, BIRD_W, BIRD_H, lane.speed*1.1));
                        toPlace--;
                    }
                } else {
                    int y = lane.y + 6, x = rng.nextInt(WIDTH-SNAKE_W);
                    if (critterLaneHasSpaceFor(lane, x, y, SNAKE_W, SNAKE_H, CRITTER_MIN_GAP)) {
                        snakes.add(new Snake(x, y, SNAKE_W, SNAKE_H, lane.speed*0.9));
                        toPlace--;
                    }
                }
            }
        }

        resetFrog(false);
    }

    private void resetFrog(boolean keepRowBonus) {
        if (!keepRowBonus) bestRowY = (ROWS-1)*TILE + 4;
        frog.x = WIDTH/2 - TILE/2;
        frog.y = (ROWS-1)*TILE + 4;
        frog.alive = true;
        repaint();
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (paused) { repaint(); return; }
        ticks++;

        if (!freePlay) {
            // Level 1: normal updates and spawns 
            for (Lane lane : roadLanes) {
                if (ticks % lane.spawnEveryTicks == 0) {
                    boolean truck = rng.nextDouble() < 0.33;
                    int y = lane.y + 5;

                    if (truck) {
                        int w=TRUCK_W, h=TRUCK_H;
                        boolean right = lane.speed > 0;
                        int x = right ? -w - 12 : WIDTH + 12;
                        double sp = vary(Math.copySign(Math.abs(lane.speed*0.85), lane.speed), 0.25);
                        int enterX = right ? -w : WIDTH;
                        if (laneHasSpaceFor(lane, enterX, y, w, h, VEHICLE_MIN_GAP))
                            trucks.add(new Truck(x, y, w, h, sp));
                    } else {
                        int w=CAR_W, h=CAR_H;
                        boolean right = lane.speed > 0;
                        int x = right ? -w - 12 : WIDTH + 12;
                        Car.Kind kind = (rng.nextDouble()<0.5) ? Car.Kind.RED : Car.Kind.NORMAL;
                        double sp = vary(lane.speed, 0.25);
                        int enterX = right ? -w : WIDTH;
                        if (laneHasSpaceFor(lane, enterX, y, w, h, VEHICLE_MIN_GAP))
                            cars.add(new Car(x, y, w, h, sp, kind));
                    }
                }
            }

            for (Lane lane : riverLanes) {
                if (ticks % lane.spawnEveryTicks == 0 && rng.nextDouble() < 0.75) {
                    int y = lane.y + 6;
                    int x = lane.speed > 0 ? -LOG_W - 10 : WIDTH + 10;
                    if (logLaneHasSpaceFor(lane, x, y, LOG_W, LOG_H, TILE/3))
                        logs.add(new Log(x, y, LOG_W, LOG_H, lane.speed));
                }
            }

            for (Lane lane : critterLanes) {
                if (ticks % lane.spawnEveryTicks == 0 && rng.nextDouble() < 0.65) {
                    boolean bird = rng.nextBoolean();
                    if (bird) {
                        int y = lane.y + 7;
                        int x = lane.speed > 0 ? -BIRD_W - 10 : WIDTH + 10;
                        if (critterLaneHasSpaceFor(lane, x, y, BIRD_W, BIRD_H, CRITTER_MIN_GAP))
                            birds.add(new Bird(x, y, BIRD_W, BIRD_H, lane.speed*1.1));
                    } else {
                        int y = lane.y + 6;
                        int x = lane.speed > 0 ? -SNAKE_W - 10 : WIDTH + 10;
                        if (critterLaneHasSpaceFor(lane, x, y, SNAKE_W, SNAKE_H, CRITTER_MIN_GAP))
                            snakes.add(new Snake(x, y, SNAKE_W, SNAKE_H, lane.speed*0.9));
                    }
                }
            }
        }

        // Move actors 
        cars.forEach(Car::update);
        trucks.forEach(Truck::update);
        logs.forEach(Log::update);
        birds.forEach(Bird::update);
        snakes.forEach(Snake::update);

        if (!freePlay) resolveVehicleGapsSingleTrack();

        // Trim off screen for performance
        cars.removeIf(c -> c.x < -c.w - 60 || c.x > WIDTH + 60);
        trucks.removeIf(t -> t.x < -t.w - 60 || t.x > WIDTH + 60);
        logs.removeIf(l -> l.x < -l.w - 60 || l.x > WIDTH + 60);
        birds.removeIf(b -> b.x < -b.w - 60 || b.x > WIDTH + 60);
        snakes.removeIf(s -> s.x < -s.w - 60 || s.x > WIDTH + 60);

        frog.update();

        // Raises points for moving up rows (only Level 1)
        if (!freePlay && frog.y < bestRowY) {
            int rowsUp = (bestRowY - frog.y) / TILE;
            if (rowsUp > 0) { score += rowsUp * 10; bestRowY = frog.y; }
        }

        if (!freePlay) {
            // Collisions with frog (only Level 1)
            Rectangle fr = frog.bounds();
            for (Car c : cars)    if (c.bounds().intersects(fr)) { die(); repaint(); return; }
            for (Truck t : trucks) if (t.bounds().intersects(fr)) { die(); repaint(); return; }
            for (Bird b : birds)   if (b.bounds().intersects(fr)) { die(); repaint(); return; }
            for (Snake s : snakes) if (s.bounds().intersects(fr)) { die(); repaint(); return; }

            // River logic (only Level 1)
            boolean inRiver = frog.y >= 2*TILE && frog.y < 5*TILE;
            if (inRiver) {
                boolean onLog = false; double carry = 0;
                for (Log l : logs) if (l.bounds().intersects(fr)) { onLog = true; carry = l.speed; break; }
                if (!onLog) { die(); repaint(); return; }
                frog.x += carry; frog.clampToBoard(WIDTH);
            }

            // Reached goal > advance to Level 2 (free move for now)
            if (frog.y <= TILE) {
                score += 100;
                level = 2;    // go to second level
                setupLevel(); // rebuild as blank frame (until updated)
                repaint();
                return;
            }
        }

        repaint();
    }

    private void die() {
        if (!frog.alive) return;
        frog.alive = false; lives--; score = Math.max(0, score - 25);
        if (lives <= 0) { level = 1; lives = 3; score = 0; }
        setupLevel();
    }

    //  Overlap prevention (Level 1 only) 
    private boolean laneHasSpaceFor(Lane lane, int x, int y, int w, int h, int gap) {
        Rectangle cand = new Rectangle(x,y,w,h);
        for (Car c : cars)    if (Math.abs(c.y - y) < 2 && expand(c.bounds(), gap).intersects(cand)) return false;
        for (Truck t : trucks) if (Math.abs(t.y - y) < 2 && expand(t.bounds(), gap).intersects(cand)) return false;
        return true;
    }
    private boolean logLaneHasSpaceFor(Lane lane, int x, int y, int w, int h, int gap) {
        Rectangle cand = new Rectangle(x,y,w,h);
        for (Log l : logs) if (Math.abs(l.y - lane.y) < TILE/2 && expand(l.bounds(), gap).intersects(cand)) return false;
        return true;
    }
    private boolean critterLaneHasSpaceFor(Lane lane, int x, int y, int w, int h, int gap) {
        Rectangle cand = new Rectangle(x,y,w,h);
        for (Bird b : birds) if (Math.abs(b.y - lane.y) < TILE/2 && expand(b.bounds(), gap).intersects(cand)) return false;
        for (Snake s : snakes) if (Math.abs(s.y - lane.y) < TILE/2 && expand(s.bounds(), gap).intersects(cand)) return false;
        return true;
    }
    private Rectangle expand(Rectangle r, int gap){ return new Rectangle(r.x-gap, r.y, r.width+2*gap, r.height); }

    private void resolveVehicleGapsSingleTrack() {
        for (Lane lane : roadLanes) {
            int laneY = lane.y + 5;
            ArrayList<Object> objs = new ArrayList<>();
            for (Car c : cars)    if (Math.abs(c.y - laneY) <= 2) objs.add(c);
            for (Truck t : trucks) if (Math.abs(t.y - laneY) <= 2) objs.add(t);
            if (objs.size() < 2) continue;

            objs.sort((a,b)->Double.compare(getX(a), getX(b)));

            for (int i=0;i<objs.size()-1;i++){
                Object A = objs.get(i);
                Object B = objs.get(i+1);
                Rectangle ra = bounds(A), rb = bounds(B);

                int needed = (ra.x + ra.width + VEHICLE_MIN_GAP) - rb.x;
                if (needed > 0) {
                    double sa = getSpeed(A);
                    double sb = getSpeed(B);
                    if (Math.signum(sa) == Math.signum(sb)) {
                        if (sa > 0) setX(B, getX(B) + needed);
                        else        setX(A, getX(A) - needed);
                    } else {
                        setX(A, getX(A) - needed/2.0);
                        setX(B, getX(B) + needed/2.0);
                    }
                }
            }
        }
    }

    // helpers for Car/Truck size
    private double getX(Object o){ return (o instanceof Car) ? ((Car)o).x : ((Truck)o).x; }
    private void   setX(Object o, double v){ if (o instanceof Car) ((Car)o).x=v; else ((Truck)o).x=v; }
    private double getSpeed(Object o){ return (o instanceof Car) ? ((Car)o).speed : ((Truck)o).speed; }
    private Rectangle bounds(Object o){
        if (o instanceof Car c)   return c.bounds();
        if (o instanceof Truck t) return t.bounds();
        return new Rectangle();
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // HUD
        g.setColor(new Color(22,18,32)); g.fillRect(0,0,WIDTH,TILE);
        g.setColor(Color.WHITE); g.setFont(g.getFont().deriveFont(Font.BOLD,18f));
        String hdr = freePlay ? "Level: 2 (Free Move)   Lives: "+lives+"   Score: "+score
                              : "Level: "+level+"   Lives: "+lives+"   Score: "+score;
        g.drawString(hdr + (paused?"   [PAUSED]":""), 10, 26);

        if (freePlay) {
            //  Level 2: blank frame background for now
            g.setColor(new Color(34, 40, 52)); 
            g.fillRect(0, TILE, WIDTH, HEIGHT - TILE);

            // Draw the frog only
            frog.draw(g);

            if (showHelp) {
                String[] lines = {"Free Move Mode", "Arrow keys to move the frog", "Press H to hide/show help"};
                int w=420,h=110,x=WIDTH/2-w/2,y=HEIGHT/2-h/2;
                g.setColor(new Color(0,0,0,180)); g.fillRect(x,y,w,h);
                g.setColor(Color.WHITE); g.drawRect(x,y,w,h);
                int yy=y+26; g.setFont(g.getFont().deriveFont(Font.PLAIN,16f));
                for (String line:lines){ g.drawString(line, x+12, yy); yy+=22; }
            }
            return;
        }

        // Level 1: draw tiles and actors 
        // Background tiles
        drawRow(g, 1, Assets.tileGoal(),  new Color(72,160,72));
        for (int r=2;r<=4;r++) drawRow(g, r, Assets.tileWater(), new Color(40,88,152));
        for (int r=6;r<=11;r++) drawRow(g, r, Assets.tileRoad(),  new Color(56,56,56));
        for (int r=14;r<=16;r++) drawRow(g, r, Assets.tileGrass(), new Color(72,160,72));
        drawRow(g, ROWS-1, Assets.tileStart(), new Color(72,160,72));

        // Road markings
        BufferedImage mark = Assets.tileRoadMark();
        if (mark != null) {
            for (Lane lane : roadLanes)
                for (int x=0;x<WIDTH;x+=TILE)
                    g.drawImage(mark, x, lane.y + TILE/2 - 2, TILE, 4, null);
        } else {
            g.setColor(new Color(236,214,96));
            for (Lane lane : roadLanes)
                for (int x=0;x<WIDTH;x+=60) g.fillRect(x, lane.y + TILE/2 - 1, 30, 2);
        }

        // Actors
        logs.forEach(l -> l.draw(g));
        trucks.forEach(t -> t.draw(g));
        cars.forEach(c -> c.draw(g));
        birds.forEach(b -> b.draw(g));
        snakes.forEach(s -> s.draw(g));
        frog.draw(g);

        if (showHelp) {
            String[] lines = {"  Arrow keys move  |  P pause  |  H help"};
            int w=300,h=100,x=WIDTH/2-w/2,y=HEIGHT/2-h/2;
            g.setColor(new Color(0,0,0,180)); g.fillRect(x,y,w,h);
            g.setColor(Color.WHITE); g.drawRect(x,y,w,h);
            int yy=y+26; g.setFont(g.getFont().deriveFont(Font.PLAIN,16f));
            for (String line:lines){ g.drawString(line, x+12, yy); yy+=22; }
        }
    }

    private void drawRow(Graphics2D g, int row, BufferedImage tile, Color fallback) {
        int y = row * TILE;
        if (tile != null) {
            for (int c=0;c<COLS;c++) g.drawImage(tile, c*TILE, y, TILE, TILE, null);
        } else {
            g.setColor(fallback);
            g.fillRect(0, y, WIDTH, TILE);
        }
    }

    // input, need to make character move side to side and back
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
