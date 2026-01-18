package assignments.Ex3;

import exe.ex3.game.GhostCL;

public class MyGhostCL implements GhostCL {
    private int x, y;
    private double eatableTimeSec = 0.0;

    public MyGhostCL(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() { return x; }
    public int y() { return y; }

    public void setXY(int nx, int ny) { x = nx; y = ny; }

    public void setEatable(double seconds) {
        eatableTimeSec = Math.max(eatableTimeSec, seconds);
    }

    public void tick(double dtSec) {
        eatableTimeSec = Math.max(0.0, eatableTimeSec - dtSec);
    }

    // used by Ex3Algo
    @Override public String getPos(int ignored) { return x + "," + y; }

    @Override public double remainTimeAsEatable(int ignored) { return eatableTimeSec; }

    // stubs (interface may require them)
    @Override public int getType() { return 0; }
    @Override public String getInfo() { return ""; }
    @Override public int getStatus() { return 0; }
}
