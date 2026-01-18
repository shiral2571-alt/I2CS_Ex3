package assignments.Ex3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

/**
 * Enhanced StdDraw for Ex3.
 * Adds text(), textLeft() convenience and keeps your scaling.
 */
public final class StdDraw {

    private static JFrame frame;
    private static DrawPanel panel;

    private static int width = 900, height = 650;
    private static double xmin = 0.0, xmax = 1.0;
    private static double ymin = 0.0, ymax = 1.0;

    private static Color penColor = Color.BLACK;
    private static volatile boolean hasKey = false;
    private static volatile char lastKey = 0;

    static { init(); }
    private StdDraw() {}

    private static void init() {
        if (frame != null) return;

        frame = new JFrame("Ex3 - Pacman Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        panel = new DrawPanel(width, height);
        frame.setContentPane(panel);

        frame.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) {
                lastKey = e.getKeyChar();
                hasKey = true;
                System.out.println("[KEY] Pressed: " + lastKey);
            }
        });

        frame.pack();
        frame.setVisible(true);
        System.out.println("[STDDRAW] Initialized with size: " + width + "x" + height);
    }

    // ---------- API ----------

    /** Double buffering is inherent in the buffered image approach. */
    public static void enableDoubleBuffering() {
        System.out.println("[STDDRAW] Double buffering enabled (automatic in this implementation).");
    }

    public static void setCanvasSize(int w, int h) {
        width = w; height = h;
        if (frame != null) frame.dispose();
        frame = null;
        init();
    }

    public static void setXscale(double min, double max) {
        xmin = min; xmax = max;
        System.out.println("[STDDRAW] X Scale updated: " + xmin + " to " + xmax);
    }

    public static void setYscale(double min, double max) {
        ymin = min; ymax = max;
        System.out.println("[STDDRAW] Y Scale updated: " + ymin + " to " + ymax);
    }

    public static void setPenColor(Color c) {
        penColor = c;
        if (panel != null) panel.g2.setColor(penColor);
    }

    public static void clear(Color c) {
        ensureInit();
        panel.g2.setColor(c);
        panel.g2.fillRect(0, 0, width, height);
        panel.g2.setColor(penColor);
    }

    public static void filledCircle(double x, double y, double r) {
        ensureInit();
        double px = scaleX(x), py = scaleY(y);
        double prW = scaleW(r), prH = scaleH(r);
        panel.g2.fillOval((int)Math.round(px - prW), (int)Math.round(py - prH),
                (int)Math.round(2*prW), (int)Math.round(2*prH));
    }

    public static void filledRectangle(double x, double y, double halfW, double halfH) {
        ensureInit();
        double px = scaleX(x), py = scaleY(y);
        double pw = scaleW(halfW), ph = scaleH(halfH);
        panel.g2.fillRect((int)Math.round(px - pw), (int)Math.round(py - ph),
                (int)Math.round(2*pw), (int)Math.round(2*ph));
    }

    public static void filledArc(double x, double y, double r, double angle1, double angle2) {
        ensureInit();
        double px = scaleX(x), py = scaleY(y), pr = scaleW(r);
        double extent = angle2 - angle1;
        Arc2D.Double arc = new Arc2D.Double(px - pr, py - pr, 2*pr, 2*pr, angle1, extent, Arc2D.PIE);
        panel.g2.fill(arc);
    }

    /**
     * Draw centered text at (x,y) in user coordinates.
     * This matches the common StdDraw API used in many courses.
     */
    public static void text(double x, double y, String s) {
        ensureInit();
        if (s == null) s = "";
        double px = scaleX(x);
        double py = scaleY(y);

        panel.g2.setColor(penColor);
        FontMetrics fm = panel.g2.getFontMetrics();
        int tw = fm.stringWidth(s);
        int th = fm.getAscent();

        // center
        int drawX = (int)Math.round(px - tw / 2.0);
        int drawY = (int)Math.round(py + th / 2.0);
        panel.g2.drawString(s, drawX, drawY);
    }

    /**
     * Convenience: draw left-aligned text starting at (x,y) in user coordinates.
     * Added so your MyPacmanServer can use textLeft(...) if you want.
     */
    public static void textLeft(double x, double y, String s) {
        ensureInit();
        if (s == null) s = "";
        double px = scaleX(x);
        double py = scaleY(y);

        panel.g2.setColor(penColor);
        FontMetrics fm = panel.g2.getFontMetrics();
        int th = fm.getAscent();

        int drawX = (int)Math.round(px);
        int drawY = (int)Math.round(py + th / 2.0);
        panel.g2.drawString(s, drawX, drawY);
    }

    public static void show() {
        ensureInit();
        panel.repaint();
        Toolkit.getDefaultToolkit().sync();
    }

    public static void pause(int ms) {
        show();
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ---------- Scaling Logic ----------

    private static double scaleX(double x) { return (x - xmin) / (xmax - xmin) * width; }
    private static double scaleY(double y) { return height - (y - ymin) / (ymax - ymin) * height; }
    private static double scaleW(double w) { return w / (xmax - xmin) * width; }
    private static double scaleH(double h) { return h / (ymax - ymin) * height; }

    private static void ensureInit() {
        if (frame == null || panel == null) init();
    }

    // ---------- Internal Panel ----------

    private static class DrawPanel extends JPanel {
        private final BufferedImage img;
        private final Graphics2D g2;

        DrawPanel(int w, int h) {
            setPreferredSize(new Dimension(w, h));
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            g2.setColor(penColor);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(img, 0, 0, null);
        }
    }

    // Keyboard handling
    public static boolean hasNextKeyTyped() { return hasKey; }
    public static char nextKeyTyped() {
        char c = lastKey;
        hasKey = false;
        return c;
    }
}
