package assignments.Ex3;

import exe.ex3.game.PacmanGame;

import java.awt.Color;
import java.util.Random;

public class MyPacmanServer implements PacmanGame {

    // internal cell codes (board[y][x])
    private static final int I_WALL  = 0;
    private static final int I_FLOOR = 1;
    private static final int I_PINK  = 2;
    private static final int I_GREEN = 3;

    private static final double GREEN_EATABLE_SEC = 6.0;

    private int status = INIT;
    private boolean cyclic = false;

    private int W, H;
    private int[][] board; // [H][W]

    private int pacX, pacY;
    private int pacLastDir = PacmanGame.RIGHT;

    private long delayMs = 60;
    private long startTime = 0;
    private long maxTimeMs = 120_000;

    private final Ex3Algo algo = new Ex3Algo();

    private MyGhostCL[] ghosts;
    private final Random rnd = new Random(123);

    //  Ghost cage (board coords)
    private int cageX1, cageY1, cageX2, cageY2; // inclusive
    private int doorX, doorY;
    private long doorOpensAtMs = 0;
    private static final long DOOR_DELAY_MS = 3500;

    // PacmanGame API

    @Override public Character getKeyChar() { return null; }

    @Override
    public String getPos(int id) {
        if (id == 0) return pacX + "," + pacY;
        return "-1,-1";
    }

    @Override
    public exe.ex3.game.GhostCL[] getGhosts(int ignored) {
        return ghosts;
    }

    @Override
    public int[][] getGame(int ignored) {
        // Ex3Algo expects board[x][y] with values: WALL=1, PINK=3, GREEN=5, floor=0
        int[][] out = new int[W][H];
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                int v = board[y][x];
                if (v == I_WALL) out[x][y] = 1;
                else if (v == I_PINK) out[x][y] = 3;
                else if (v == I_GREEN) out[x][y] = 5;
                else out[x][y] = 0;
            }
        }
        return out;
    }

    @Override
    public String move(int dir) {
        if (status != PLAY) return "ERR:not playing";

        // ---- Anti-stuck: if algo suggests illegal move, pick a legal fallback ----
        dir = fallbackDir(dir);
        if (dir == 0) {
            // can't move anywhere: still tick ghosts + collisions
            tickGhosts();
            if (checkCollision()) {
                status = DONE;
                return "DONE:collision";
            }
            return "OK";
        }

        // pacman move
        int nx = pacX, ny = pacY;
        if (dir == LEFT) nx--;
        else if (dir == RIGHT) nx++;
        else if (dir == UP) ny--;
        else if (dir == DOWN) ny++;

        if (cyclic) {
            nx = (nx + W) % W;
            ny = (ny + H) % H;
        } else {
            if (nx < 0 || ny < 0 || nx >= W || ny >= H) return "OK";
        }

        // no overlap: pacman cannot step into ghost
        if (!isGhostAt(nx, ny) && board[ny][nx] != I_WALL) {
            pacX = nx; pacY = ny;
            pacLastDir = dir;
        }

        // eat dots
        if (board[pacY][pacX] == I_PINK) board[pacY][pacX] = I_FLOOR;
        if (board[pacY][pacX] == I_GREEN) {
            board[pacY][pacX] = I_FLOOR;
            for (MyGhostCL g : ghosts) g.setEatable(GREEN_EATABLE_SEC);
        }

        // ghosts tick
        tickGhosts();

        // collision
        if (checkCollision()) {
            status = DONE;
            return "DONE:collision";
        }

        // win
        if (count(I_PINK) == 0) {
            status = DONE;
            return "DONE:win";
        }

        // time
        if (System.currentTimeMillis() - startTime > maxTimeMs) {
            status = DONE;
            return "DONE:time";
        }

        return "OK";
    }

    @Override
    public void play() {
        if (board == null) init(0, "maze", false, delayMs, maxTimeMs / 1000.0, 0, 0);

        status = PLAY;
        startTime = System.currentTimeMillis();

        StdDraw.setCanvasSize(950, 700);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);

        while (status == PLAY) {
            int dirFromAlgo = algo.move(this);
            int dir = translateDir(dirFromAlgo);

            move(dir);
            draw();

            StdDraw.pause((int) delayMs);
        }

        draw();
        StdDraw.pause(600);
    }

    @Override public String end(int code) { status = DONE; return "DONE"; }

    @Override
    public String getData(int ignored) {
        return "pos=" + pacX + "," + pacY +
                " pinkLeft=" + count(I_PINK) +
                " greenLeft=" + count(I_GREEN) +
                " status=" + status;
    }

    @Override public int getStatus() { return status; }
    @Override public boolean isCyclic() { return cyclic; }

    @Override
    public String init(int level, String mapName, boolean isCyclic, long delay, double maxTimeSeconds, int var8, int var9) {
        cyclic = isCyclic;
        delayMs = delay;
        maxTimeMs = (long) (maxTimeSeconds * 1000);

        buildMazeSingleCellWithPlaza();   // smaller maze
        placePacmanFixed();
        placeGreensExtra(3);
        placeGhostsInCage(4);

        doorOpensAtMs = System.currentTimeMillis() + DOOR_DELAY_MS;

        status = INIT;
        return "OK:init";
    }

    //  internal

    private int translateDir(int d) {
        // Ex3Algo: DOWN=1 LEFT=2 UP=3 RIGHT=4
        // PacmanGame: UP=1 LEFT=2 DOWN=3 RIGHT=4
        if (d == 1) return PacmanGame.DOWN;
        if (d == 3) return PacmanGame.UP;
        return d;
    }

    //  Anti-stuck helpers
    private boolean canPacStep(int x, int y, int dir) {
        int nx = x, ny = y;
        if (dir == LEFT) nx--;
        else if (dir == RIGHT) nx++;
        else if (dir == UP) ny--;
        else if (dir == DOWN) ny++;

        if (cyclic) {
            nx = (nx + W) % W;
            ny = (ny + H) % H;
        } else {
            if (nx < 0 || ny < 0 || nx >= W || ny >= H) return false;
        }

        if (board[ny][nx] == I_WALL) return false;
        if (isGhostAt(nx, ny)) return false;
        return true;
    }

    private int fallbackDir(int preferred) {
        // try preferred, then keep current direction, then try others
        int[] dirs = {preferred, pacLastDir, UP, LEFT, DOWN, RIGHT};
        for (int d : dirs) {
            if (d == 0) continue;
            if (canPacStep(pacX, pacY, d)) return d;
        }
        return 0; // stuck for real
    }

    /**
     * Builds:
     * - single-cell corridors maze (DFS perfect maze)
     * - big open plaza in the middle
     * - adds loops (extra openings) WITHOUT creating 2x2 open blocks
     * - builds a ghost cage with a door
     * - fills dots only on corridors (one dot per cell)
     *
     * SMALLER VERSION: 23x17 (was 31x23)
     */
    private void buildMazeSingleCellWithPlaza() {
        // ---- Smaller maze (still odd for 1-cell corridors) ----
        W = 23;
        H = 17;
        board = new int[H][W];

        // start as all walls
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                board[y][x] = I_WALL;

        // carve perfect maze on odd cells
        carvePerfectMazeDFS(1, 1);

        //  plaza (scaled)
        int plazaW = Math.min(9,  W - 8);
        int plazaH = Math.min(7,  H - 6);
        if (plazaW % 2 == 0) plazaW--;
        if (plazaH % 2 == 0) plazaH--;

        int px1 = (W - plazaW) / 2;
        int py1 = (H - plazaH) / 2;
        int px2 = px1 + plazaW - 1;
        int py2 = py1 + plazaH - 1;

        // make plaza open
        for (int y = py1; y <= py2; y++)
            for (int x = px1; x <= px2; x++)
                board[y][x] = I_FLOOR;

        // connect plaza to maze in 4 places
        openPlazaGate(px1, (py1+py2)/2);
        openPlazaGate(px2, (py1+py2)/2);
        openPlazaGate((px1+px2)/2, py1);
        openPlazaGate((px1+px2)/2, py2);

        //  ghost cage inside the plaza (centered)
        cageX1 = (W/2) - 2;
        cageX2 = (W/2) + 2;
        cageY1 = (H/2) - 1;
        cageY2 = (H/2) + 1;

        // build cage box
        for (int y = cageY1; y <= cageY2; y++)
            for (int x = cageX1; x <= cageX2; x++)
                board[y][x] = I_FLOOR;

        for (int x = cageX1; x <= cageX2; x++) {
            board[cageY1][x] = I_WALL;
            board[cageY2][x] = I_WALL;
        }
        for (int y = cageY1; y <= cageY2; y++) {
            board[y][cageX1] = I_WALL;
            board[y][cageX2] = I_WALL;
        }

        // door at top middle
        doorX = (cageX1 + cageX2) / 2;
        doorY = cageY1;
        board[doorY][doorX] = I_FLOOR;

        // clear around cage so it isn't cramped
        for (int y = cageY1-1; y <= cageY2+1; y++)
            for (int x = cageX1-1; x <= cageX2+1; x++)
                if (inBounds(x,y) && board[y][x] != I_WALL) board[y][x] = I_FLOOR;

        //  loosen maze: add loops (scaled)
        int openings = (W * H) / 12;
        addLoops(openings);

        //  place dots
        fillDots();

        // 2 greens at corners
        board[1][W-2] = I_GREEN;
        board[H-2][1] = I_GREEN;

        // clear dots inside plaza
        for (int y = py1; y <= py2; y++)
            for (int x = px1; x <= px2; x++)
                if (board[y][x] == I_PINK) board[y][x] = I_FLOOR;
    }

    // DFS perfect maze: carve on odd cells, open walls between.
    private void carvePerfectMazeDFS(int sx, int sy) {
        board[sy][sx] = I_FLOOR;

        int[] dirs = {0,1,2,3}; // 0R 1L 2D 3U
        shuffle4(dirs);

        for (int d : dirs) {
            int dx = 0, dy = 0;
            if (d == 0) dx = 2;
            if (d == 1) dx = -2;
            if (d == 2) dy = 2;
            if (d == 3) dy = -2;

            int nx = sx + dx;
            int ny = sy + dy;

            if (!inBounds(nx, ny)) continue;
            if (board[ny][nx] != I_WALL) continue;

            board[sy + dy/2][sx + dx/2] = I_FLOOR;
            board[ny][nx] = I_FLOOR;

            carvePerfectMazeDFS(nx, ny);
        }
    }

    private void shuffle4(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }

    private void openPlazaGate(int x, int y) {
        if (!inBounds(x, y)) return;
        board[y][x] = I_FLOOR;

        int cx = W/2, cy = H/2;
        int ox = x, oy = y;
        if (Math.abs(x - cx) > Math.abs(y - cy)) {
            ox += (x < cx) ? -1 : 1;
        } else {
            oy += (y < cy) ? -1 : 1;
        }
        if (inBounds(ox, oy)) board[oy][ox] = I_FLOOR;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < W && y < H;
    }

    private void fillDots() {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (board[y][x] == I_FLOOR) board[y][x] = I_PINK;
            }
        }
    }

    // Adds extra openings, prevents 2x2 open blocks
    private void addLoops(int openings) {
        int tries = 0;
        int made = 0;

        while (made < openings && tries < openings * 250) {
            tries++;

            int x = 1 + rnd.nextInt(W - 2);
            int y = 1 + rnd.nextInt(H - 2);

            if (board[y][x] != I_WALL) continue;

            boolean horiz = (board[y][x - 1] != I_WALL && board[y][x + 1] != I_WALL);
            boolean vert  = (board[y - 1][x] != I_WALL && board[y + 1][x] != I_WALL);
            if (!horiz && !vert) continue;

            if (isInOrNearCage(x, y)) continue;
            if (wouldCreate2x2Open(x, y)) continue;

            board[y][x] = I_FLOOR;
            made++;
        }
    }

    private boolean wouldCreate2x2Open(int x, int y) {
        for (int dy = -1; dy <= 0; dy++) {
            for (int dx = -1; dx <= 0; dx++) {
                int x0 = x + dx;
                int y0 = y + dy;
                if (!inBounds(x0, y0) || !inBounds(x0+1, y0+1)) continue;

                int open = 0;
                open += ((x0 == x && y0 == y) || board[y0][x0] != I_WALL) ? 1 : 0;
                open += ((x0+1 == x && y0 == y) || board[y0][x0+1] != I_WALL) ? 1 : 0;
                open += ((x0 == x && y0+1 == y) || board[y0+1][x0] != I_WALL) ? 1 : 0;
                open += ((x0+1 == x && y0+1 == y) || board[y0+1][x0+1] != I_WALL) ? 1 : 0;

                if (open == 4) return true;
            }
        }
        return false;
    }

    private boolean isInOrNearCage(int x, int y) {
        return x >= cageX1 - 2 && x <= cageX2 + 2 && y >= cageY1 - 2 && y <= cageY2 + 2;
    }

    private void placePacmanFixed() {
        pacX = 1; pacY = 1;
        pacLastDir = PacmanGame.RIGHT;
        if (board[pacY][pacX] != I_WALL) board[pacY][pacX] = I_FLOOR;
    }

    private void placeGreensExtra(int k) {
        int placed = 0;
        while (placed < k) {
            int x = 1 + rnd.nextInt(W - 2);
            int y = 1 + rnd.nextInt(H - 2);
            if (board[y][x] == I_PINK) {
                board[y][x] = I_GREEN;
                placed++;
            }
        }
    }

    private void placeGhostsInCage(int n) {
        ghosts = new MyGhostCL[n];
        int idx = 0;

        for (int y = cageY1 + 1; y <= cageY2 - 1 && idx < n; y++) {
            for (int x = cageX1 + 1; x <= cageX2 - 1 && idx < n; x++) {
                ghosts[idx++] = new MyGhostCL(x, y);
            }
        }
        while (idx < n) ghosts[idx++] = new MyGhostCL(doorX, cageY2 - 1);
    }

    private void tickGhosts() {
        double dt = delayMs / 1000.0;
        long now = System.currentTimeMillis();

        for (MyGhostCL g : ghosts) g.tick(dt);

        for (MyGhostCL g : ghosts) {
            int gx = g.x(), gy = g.y();
            boolean eatable = g.remainTimeAsEatable(0) > 0.05;

            int bestDir = 0;
            int bestScore = eatable ? Integer.MIN_VALUE : Integer.MAX_VALUE;

            int[] dirs = {UP, DOWN, LEFT, RIGHT};
            for (int d : dirs) {
                int[] n = step(gx, gy, d);
                if (n == null) continue;

                // door rule: until opens, cannot leave cage
                if (now < doorOpensAtMs) {
                    boolean fromInside = isInsideCage(gx, gy);
                    boolean toInside   = isInsideCage(n[0], n[1]);
                    if (fromInside && !toInside) continue;
                }

                // no overlap
                if (n[0] == pacX && n[1] == pacY) continue;
                if (isOtherGhostAt(g, n[0], n[1])) continue;

                int man = Math.abs(n[0] - pacX) + Math.abs(n[1] - pacY);

                if (eatable) {
                    if (man > bestScore) { bestScore = man; bestDir = d; }
                } else {
                    if (man < bestScore) { bestScore = man; bestDir = d; }
                }
            }

            if (bestDir != 0) {
                int[] n = step(gx, gy, bestDir);
                if (n != null) g.setXY(n[0], n[1]);
            }
        }
    }

    private int[] step(int x, int y, int d) {
        int nx = x, ny = y;
        if (d == LEFT) nx--;
        else if (d == RIGHT) nx++;
        else if (d == UP) ny--;
        else if (d == DOWN) ny++;

        if (cyclic) {
            nx = (nx + W) % W;
            ny = (ny + H) % H;
        } else {
            if (nx < 0 || ny < 0 || nx >= W || ny >= H) return null;
        }

        if (board[ny][nx] == I_WALL) return null;
        return new int[]{nx, ny};
    }

    private boolean checkCollision() {
        for (MyGhostCL g : ghosts) {
            if (g.x() == pacX && g.y() == pacY) {
                if (g.remainTimeAsEatable(0) > 0.05) {
                    g.setXY(doorX, cageY2 - 1);
                    g.setEatable(0);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private int count(int type) {
        int c = 0;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                if (board[y][x] == type) c++;
        return c;
    }

    private boolean isGhostAt(int x, int y) {
        if (ghosts == null) return false;
        for (MyGhostCL g : ghosts) if (g.x() == x && g.y() == y) return true;
        return false;
    }

    private boolean isInsideCage(int x, int y) {
        return x >= cageX1 && x <= cageX2 && y >= cageY1 && y <= cageY2;
    }

    private boolean isOtherGhostAt(MyGhostCL me, int x, int y) {
        for (MyGhostCL g : ghosts) {
            if (g == me) continue;
            if (g.x() == x && g.y() == y) return true;
        }
        return false;
    }

    //  drawing

    private void draw() {
        StdDraw.clear(Color.BLACK);

        double cellW = 1.0 / W;
        double cellH = 1.0 / H;

        // walls: dark + bright outline
        StdDraw.setPenColor(new Color(0, 0, 120));
        drawWallOutlines(cellW, cellH, 0.07);

        StdDraw.setPenColor(Color.BLUE);
        drawWallOutlines(cellW, cellH, 0.045);

        // dots
        double rDot   = Math.min(cellW, cellH) * 0.10;
        double rGreen = Math.min(cellW, cellH) * 0.20;

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (board[y][x] != I_PINK && board[y][x] != I_GREEN) continue;

                double cx = (x + 0.5) * cellW;
                double cy = 1.0 - (y + 0.5) * cellH;

                if (board[y][x] == I_PINK) {
                    StdDraw.setPenColor(new Color(255, 190, 200));
                    StdDraw.filledCircle(cx, cy, rDot);
                } else {
                    StdDraw.setPenColor(Color.GREEN);
                    StdDraw.filledCircle(cx, cy, rGreen);
                }
            }
        }

        // pacman: arc mouth
        double pcx = (pacX + 0.5) * cellW;
        double pcy = 1.0 - (pacY + 0.5) * cellH;
        double rPac = Math.min(cellW, cellH) * 0.42;

        double ang = 0;
        if (pacLastDir == LEFT) ang = 180;
        else if (pacLastDir == UP) ang = 90;
        else if (pacLastDir == DOWN) ang = 270;

        double mouth = 55;
        StdDraw.setPenColor(Color.YELLOW);
        StdDraw.filledArc(pcx, pcy, rPac, ang + mouth, ang + 360 - mouth);

        // ghosts
        Color[] classic = {
                new Color(255, 140, 0),
                Color.RED,
                Color.PINK,
                Color.CYAN
        };

        for (int i = 0; i < ghosts.length; i++) {
            MyGhostCL g = ghosts[i];

            double gx = (g.x() + 0.5) * cellW;
            double gy = 1.0 - (g.y() + 0.5) * cellH;

            double rG = Math.min(cellW, cellH) * 0.42;
            boolean eatable = g.remainTimeAsEatable(0) > 0.05;

            Color body = eatable ? Color.CYAN : classic[i % classic.length];
            drawGhostTeacher(gx, gy, rG, body);
        }

        // HUD
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.textLeft(0.01, 0.03,
                "pinkLeft=" + count(I_PINK) +
                        " greenLeft=" + count(I_GREEN) +
                        " status=" + status);
    }

    private void drawWallOutlines(double cellW, double cellH, double thickFactor) {
        double thick = Math.min(cellW, cellH) * thickFactor;

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (board[y][x] != I_WALL) continue;

                double x0 = x * cellW;
                double x1 = (x + 1) * cellW;
                double y0 = 1.0 - (y + 1) * cellH;
                double y1 = 1.0 - y * cellH;

                if (y == 0 || board[y-1][x] != I_WALL)
                    StdDraw.filledRectangle((x0+x1)/2, y1, (cellW/2), thick);

                if (y == H-1 || board[y+1][x] != I_WALL)
                    StdDraw.filledRectangle((x0+x1)/2, y0, (cellW/2), thick);

                if (x == 0 || board[y][x-1] != I_WALL)
                    StdDraw.filledRectangle(x0, (y0+y1)/2, thick, (cellH/2));

                if (x == W-1 || board[y][x+1] != I_WALL)
                    StdDraw.filledRectangle(x1, (y0+y1)/2, thick, (cellH/2));
            }
        }
    }

    private void drawGhostTeacher(double cx, double cy, double r, Color body) {
        double headY = cy + r * 0.10;

        StdDraw.setPenColor(body);
        StdDraw.filledCircle(cx, headY, r);
        StdDraw.filledRectangle(cx, cy - r * 0.35, r, r * 0.60);

        StdDraw.setPenColor(Color.BLACK);
        double bumpR = r * 0.28;
        double baseY = cy - r * 0.78;

        StdDraw.filledCircle(cx - r * 0.60, baseY, bumpR);
        StdDraw.filledCircle(cx - r * 0.20, baseY, bumpR);
        StdDraw.filledCircle(cx + r * 0.20, baseY, bumpR);
        StdDraw.filledCircle(cx + r * 0.60, baseY, bumpR);

        StdDraw.setPenColor(Color.WHITE);
        double eyeR = r * 0.22;
        double eyeY = cy + r * 0.22;

        StdDraw.filledCircle(cx - r * 0.28, eyeY, eyeR);
        StdDraw.filledCircle(cx + r * 0.28, eyeY, eyeR);

        StdDraw.setPenColor(new Color(30, 80, 255));
        double pupR = r * 0.10;
        StdDraw.filledCircle(cx - r * 0.25, eyeY, pupR);
        StdDraw.filledCircle(cx + r * 0.25, eyeY, pupR);
    }
}
