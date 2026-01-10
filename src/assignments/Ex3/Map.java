package assignments.Ex3;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class Map implements Map2D {
    private int[][] _map;
    private boolean _cyclicFlag = true;

    public Map(int w, int h, int v) { init(w, h, v); }
    public Map(int size) { this(size, size, 0); }
    public Map(int[][] data) { init(data); }

    @Override
    public void init(int w, int h, int v) {
        _map = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                _map[i][j] = v;
            }
        }
    }

    @Override
    public void init(int[][] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null) return;
        _map = new int[arr.length][arr[0].length];
        for (int i = 0; i < arr.length; i++) {
            _map[i] = arr[i].clone();
        }
    }


    @Override
    public int[][] getMap() {
        int[][] copy = new int[_map.length][_map[0].length];
        for (int i = 0; i < _map.length; i++) {
            copy[i] = _map[i].clone();
        }
        return copy;
    }

    @Override
    public int getWidth() { return _map.length; }

    @Override
    public int getHeight() { return _map[0].length; }

    @Override
    public int getPixel(int x, int y) { return _map[x][y]; }

    @Override
    public int getPixel(Pixel2D p) { return this.getPixel(p.getX(), p.getY()); }

    @Override
    public void setPixel(int x, int y, int v) { _map[x][y] = v; }

    @Override
    public void setPixel(Pixel2D p, int v) { setPixel(p.getX(), p.getY(), v); }

    @Override
    public int fill(Pixel2D xy, int new_v) {
        int old_v = getPixel(xy);
        if (old_v == new_v) return 0;

        int count = 0;
        ArrayDeque<Pixel2D> q = new ArrayDeque<>();
        q.add(xy);
        setPixel(xy, new_v);
        count++;

        while (!q.isEmpty()) {
            Pixel2D curr = q.poll();
            Pixel2D[] neighbors = getNeighbors(curr);
            for (Pixel2D next : neighbors) {
                if (isInside(next) && getPixel(next) == old_v) {
                    setPixel(next, new_v);
                    q.add(next);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public Map2D allDistance(Pixel2D start, int obsColor) {
        Map res = new Map(getWidth(), getHeight(), -1);
        ArrayDeque<Pixel2D> q = new ArrayDeque<>();

        res.setPixel(start, 0);
        q.add(start);

        while (!q.isEmpty()) {
            Pixel2D curr = q.poll();
            int d = res.getPixel(curr);

            for (Pixel2D next : getNeighbors(curr)) {
                if (isInside(next) && getPixel(next) != obsColor && res.getPixel(next) == -1) {
                    res.setPixel(next, d + 1);
                    q.add(next);
                }
            }
        }
        return res;
    }

    @Override
    public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
        Map2D distMap = allDistance(p2, obsColor); // מתחילים מ-p2 כדי לחזור ל-p1
        if (distMap.getPixel(p1) == -1) return null;

        ArrayList<Pixel2D> path = new ArrayList<>();
        Pixel2D curr = p1;
        path.add(curr);

        while (distMap.getPixel(curr) != 0) {
            boolean moved = false;

            for (Pixel2D next : getNeighbors(curr)) {
                if (isInside(next) && distMap.getPixel(next) == distMap.getPixel(curr) - 1) {
                    curr = next;
                    path.add(curr);
                    moved = true;
                    break;
                }
            }

            if (!moved) return null; // safety: no valid step found
        }
        return path.toArray(new Pixel2D[0]);
    }

    @Override
    public boolean isInside(Pixel2D p) {
        return p != null && p.getX() >= 0 && p.getX() < getWidth() && p.getY() >= 0 && p.getY() < getHeight();
    }

    @Override
    public boolean isCyclic() { return _cyclicFlag; }

    @Override
    public void setCyclic(boolean cy) { _cyclicFlag = cy; }

    // פונקציית עזר לשכנים כולל מצב Cyclic
    private Pixel2D[] getNeighbors(Pixel2D p) {
        int x = p.getX(), y = p.getY();
        int w = getWidth(), h = getHeight();
        ArrayList<Pixel2D> neighbors = new ArrayList<>();
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};

        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (_cyclicFlag) {
                nx = (nx + w) % w;
                ny = (ny + h) % h;
            }
            neighbors.add(new Index2D(nx, ny));
        }
        return neighbors.toArray(new Pixel2D[0]);
    }
}