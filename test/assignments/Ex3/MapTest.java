package assignments.Ex3;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MapTest {

    @Test
    void testInitSize() {
        Map m = new Map(5, 4, 0);
        assertEquals(5, m.getWidth());
        assertEquals(4, m.getHeight());
    }

    @Test
    void testSetAndGetPixel() {
        Map m = new Map(3, 3, 0);
        m.setPixel(1, 1, 7);
        assertEquals(7, m.getPixel(1, 1));
    }

    @Test
    void testFill() {
        int[][] data = {
                {1, 1, 1},
                {1, 0, 1},
                {1, 1, 1}
        };
        Map m = new Map(data);
        int filled = m.fill(new Index2D(1, 1), 2);
        assertEquals(1, filled);
        assertEquals(2, m.getPixel(1, 1));
    }

    @Test
    void testAllDistanceSimple() {
        int[][] data = {
                {0, 0, 0},
                {0, 1, 0},
                {0, 0, 0}
        };
        Map m = new Map(data);
        m.setCyclic(false);
        Map2D dist = m.allDistance(new Index2D(0, 0), 1);

        assertEquals(0, dist.getPixel(0, 0));
        assertEquals(1, dist.getPixel(1, 0));
        assertEquals(2, dist.getPixel(2, 0));
    }

    @Test
    void testShortestPath() {
        int[][] data = {
                {0, 0, 0},
                {1, 1, 0},
                {0, 0, 0}
        };
        Map m = new Map(data);
        Pixel2D[] path = m.shortestPath(
                new Index2D(0, 0),
                new Index2D(2, 2),
                1
        );

        assertNotNull(path);
        assertEquals(new Index2D(0, 0), path[0]);
        assertEquals(new Index2D(2, 2), path[path.length - 1]);
    }
}