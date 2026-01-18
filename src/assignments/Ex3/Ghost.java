package assignments.Ex3;

public class Ghost {
    private int x, y;
    private boolean eatable;

    public Ghost(int x, int y) {
        this.x = x;
        this.y = y;
        this.eatable = false;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isEatable() { return eatable; }
    public void setEatable(boolean eatable) { this.eatable = eatable; }
}
