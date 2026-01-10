package assignments.Ex3;

import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;
import exe.ex3.game.GhostCL;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * This is the major algorithmic class for Ex3 - the PacMan game:
 *
 * This code is a very simple example (random-walk algorithm).
 * Your task is to implement (here) your PacMan algorithm.
 */
public class Ex3Algo implements PacManAlgo {

    // Board values
    private static final int WALL  = 1;
    private static final int PINK  = 3;
    private static final int GREEN = 5;

    // Directions
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int UP   = 3;
    private static final int RIGHT= 4;

    // Rule parameters
    private static final int DANGER_RADIUS = 4;
    private static final int GREEN_RADIUS  = 7;

    // RULE2 safety
    private static final double MIN_EATABLE_TIME = 1.2; // מתחת לזה לא רודפים בכלל
    private static final int    EAT_TIME_MARGIN  = 2;   // מרווח צעדים ביטחון (גם להגיע וגם לצאת)
    private static final int    CLUSTER_RADIUS   = 3;   // רדיוס לבדוק סביב רוח היעד
    private static final int    CLUSTER_NEEDED   = 2;   // כמה רוחות קרובות כדי להחשיב "כלוב/אשכול"

    private int lastDir = DOWN;
    private int stepCounter = 0;

    private final Deque<String> history = new ArrayDeque<>();
    private static final int HISTORY_SIZE = 10;

    private static final boolean DEBUG = false;

    /**
     * This ia the main method - that you should design, implement and test.
     */
    @Override
    public int move(PacmanGame game) {
        if (game == null) return lastDir;
        stepCounter++;

        int[][] board = game.getGame(0);
        boolean cyclic = safeCyclic(game);

        int[] p = parseXY(game.getPos(0));
        int px = p[0], py = p[1];
        if (px < 0) return lastDir;

        history.addLast(px + "," + py);
        while (history.size() > HISTORY_SIZE) history.removeFirst();

        int[][] distFromPac = bfs(board, px, py, cyclic);
        GhostCL[] ghosts = safeGhosts(game);

        // RULE 1: ESCAPE
        GhostInfo danger = closestDangerGhost(ghosts, distFromPac);
        if (danger != null && danger.dist <= DANGER_RADIUS) {
            int d = escape(board, px, py, cyclic, ghosts);
            log(px, py, d, "RULE1_ESCAPE dangerDist=" + danger.dist);
            return lastDir = d;
        }

        // RULE 2: EAT EATABLE GHOST (SAFE)
        GhostInfo eatable = closestEatableGhostSafe(ghosts, distFromPac);
        if (eatable != null) {
            int d = toward(board, px, py, eatable.gx, eatable.gy, cyclic);
            if (d != -1) {
                log(px, py, d, "RULE2_EAT_GHOST dist=" + eatable.dist + " time=" + String.format("%.2f", eatable.time));
                return lastDir = d;
            }
        }

        // RULE 3: GO TO GREEN
        int[] green = closestByBFS(board, distFromPac, GREEN, GREEN_RADIUS);
        if (green != null) {
            int d = toward(board, px, py, green[0], green[1], cyclic);
            if (d != -1) {
                log(px, py, d, "RULE3_GO_GREEN target=(" + green[0] + "," + green[1] + ")");
                return lastDir = d;
            }
        }

        // RULE 4: EAT PINK
        int[] pink = closestByBFS(board, distFromPac, PINK, Integer.MAX_VALUE);
        if (pink != null) {
            int d = toward(board, px, py, pink[0], pink[1], cyclic);
            if (d != -1) {
                log(px, py, d, "RULE4_EAT_PINK target=(" + pink[0] + "," + pink[1] + ")");
                return lastDir = d;
            }
        }

        int d = explore(board, px, py, cyclic);
        log(px, py, d, "FALLBACK_EXPLORE");
        return lastDir = d;
    }
    /**
     *  Add a short description for the algorithm as a String.
     */
    @Override
    public String getInfo() {
        return "Escape > EatGhostSafe > Green > Pink";
    }


    private static class GhostInfo {
        int gx, gy, dist;
        double time;
        GhostInfo(int x, int y, int d, double t) {
            gx = x; gy = y; dist = d; time = t;
        }
    }

    private GhostInfo closestDangerGhost(GhostCL[] ghosts, int[][] dist) {
        if (ghosts == null) return null;
        GhostInfo best = null;

        for (GhostCL g : ghosts) {
            int[] gp = parseXY(g.getPos(0));
            int gx = gp[0], gy = gp[1];

            // bounds check BEFORE dist[gx][gy]
            if (gx < 0 || gy < 0 || gx >= dist.length || gy >= dist[0].length) continue;

            double t = safeRemain(g);
            if (t > 0.1) continue; // מסוכן = לא אכיל

            int d = dist[gx][gy];
            if (d < 0) continue;

            if (best == null || d < best.dist) best = new GhostInfo(gx, gy, d, t);
        }
        return best;
    }

    // RULE 2  אכילת רוחות בטוחה
    private GhostInfo closestEatableGhostSafe(GhostCL[] ghosts, int[][] dist) {
        if (ghosts == null) return null;
        GhostInfo best = null;

        for (GhostCL g : ghosts) {
            int[] gp = parseXY(g.getPos(0));
            int gx = gp[0], gy = gp[1];

            //  bounds check BEFORE dist[gx][gy]
            if (gx < 0 || gy < 0 || gx >= dist.length || gy >= dist[0].length) continue;

            double t = safeRemain(g);
            if (t <= 0.05) continue;              // לא באמת אכיל
            if (t < MIN_EATABLE_TIME) continue;   // זמן קצר מדי לא לרדוף

            int d = dist[gx][gy];
            if (d < 0) continue;

            // חייב להספיק להגיע וגם להשאיר מרווח ביטחון
            if (d + EAT_TIME_MARGIN > t) continue;

            // Anti-pen: אם סביב הרוח יש הרבה רוחות בדרך כלל מדובר באזור הכלוב/אשכול - מסוכן להיתקע שם
            if (isGhostCluster(gx, gy, ghosts, CLUSTER_RADIUS, CLUSTER_NEEDED)) continue;

            if (best == null || d < best.dist) best = new GhostInfo(gx, gy, d, t);
        }
        return best;
    }

    private boolean isGhostCluster(int x, int y, GhostCL[] ghosts, int radius, int needed) {
        if (ghosts == null) return false;
        int count = 0;
        for (GhostCL g : ghosts) {
            int[] gp = parseXY(g.getPos(0));
            int gx = gp[0], gy = gp[1];
            int man = Math.abs(gx - x) + Math.abs(gy - y);
            if (man <= radius) count++;
            if (count >= needed) return true;
        }
        return false;
    }

    private int escape(int[][] board, int x, int y, boolean cyclic, GhostCL[] ghosts) {
        int best = lastDir;
        int bestScore = -1;

        for (int d : dirs()) {
            int[] n = step(board, x, y, d, cyclic);
            if (n == null) continue;

            int[][] dist = bfs(board, n[0], n[1], cyclic);
            int min = Integer.MAX_VALUE;

            for (GhostCL g : ghosts) {
                if (safeRemain(g) > 0.1) continue; // רק מסוכנות
                int[] gp = parseXY(g.getPos(0));
                int gx = gp[0], gy = gp[1];

                // guard גם פה (לא חובה אבל בטוח)
                if (gx < 0 || gy < 0 || gx >= dist.length || gy >= dist[0].length) continue;

                int dd = dist[gx][gy];
                if (dd >= 0) min = Math.min(min, dd);
            }

            if (min > bestScore) {
                bestScore = min;
                best = d;
            }
        }
        return best;
    }

    private int explore(int[][] board, int x, int y, boolean cyclic) {
        for (int d : dirs()) {
            int[] n = step(board, x, y, d, cyclic);
            if (n != null && !history.contains(n[0] + "," + n[1])) return d;
        }
        return lastDir;
    }

    private int toward(int[][] board, int sx, int sy, int tx, int ty, boolean cyclic) {
        int[][] dist = bfs(board, tx, ty, cyclic);
        int best = -1, val = Integer.MAX_VALUE;

        for (int d : dirs()) {
            int[] n = step(board, sx, sy, d, cyclic);
            if (n == null) continue;
            int v = dist[n[0]][n[1]];
            if (v >= 0 && v < val) {
                val = v;
                best = d;
            }
        }
        return best;
    }

    private int[] closestByBFS(int[][] board, int[][] dist, int type, int max) {
        int best = Integer.MAX_VALUE;
        int[] res = null;

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                if (board[x][y] == type && dist[x][y] >= 0 && dist[x][y] <= max) {
                    if (dist[x][y] < best) {
                        best = dist[x][y];
                        res = new int[]{x, y};
                    }
                }
            }
        }
        return res;
    }


    private int[][] bfs(int[][] board, int sx, int sy, boolean cyclic) {
        int W = board.length, H = board[0].length;
        int[][] d = new int[W][H];
        for (int[] r : d) Arrays.fill(r, -1);

        ArrayDeque<int[]> q = new ArrayDeque<>();
        d[sx][sy] = 0;
        q.add(new int[]{sx, sy});

        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int dir : dirs()) {
                int[] n = step(board, c[0], c[1], dir, cyclic);
                if (n != null && d[n[0]][n[1]] == -1) {
                    d[n[0]][n[1]] = d[c[0]][c[1]] + 1;
                    q.add(n);
                }
            }
        }
        return d;
    }

    private int[] step(int[][] board, int x, int y, int d, boolean cyclic) {
        int nx = x, ny = y;
        if (d == DOWN) ny++;
        if (d == UP) ny--;
        if (d == LEFT) nx--;
        if (d == RIGHT) nx++;

        int W = board.length, H = board[0].length;

        if (cyclic) {
            nx = (nx + W) % W;
            ny = (ny + H) % H;
        } else {
            if (nx < 0 || ny < 0 || nx >= W || ny >= H) return null;
        }

        if (board[nx][ny] == WALL) return null;
        return new int[]{nx, ny};
    }

    private int[] dirs() { return new int[]{DOWN, LEFT, UP, RIGHT}; }

    private int[] parseXY(String s) {
        Matcher m = Pattern.compile("-?\\d+").matcher(s);
        int[] r = new int[2];
        int i = 0;
        while (m.find() && i < 2) r[i++] = Integer.parseInt(m.group());
        return (i == 2) ? r : new int[]{-1, -1};
    }

    private GhostCL[] safeGhosts(PacmanGame g) {
        try { return g.getGhosts(0); } catch (Exception e) { return null; }
    }

    private boolean safeCyclic(PacmanGame g) {
        try { return g.isCyclic(); } catch (Exception e) { return true; }
    }

    private double safeRemain(GhostCL g) {
        try { return g.remainTimeAsEatable(0); } catch (Exception e) { return 0; }
    }

    private void log(int x, int y, int d, String reason) {
        if (!DEBUG) return;
        if (stepCounter % 5 != 0) return;

        String s = (d == 1 ? "DOWN" : d == 2 ? "LEFT" : d == 3 ? "UP" : "RIGHT");
        System.out.println("STEP " + stepCounter +
                " | pos=(" + x + "," + y + ")" +
                " | dir=" + s +
                " | " + reason);
    }

}
