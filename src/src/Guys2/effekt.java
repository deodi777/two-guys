package Guys2;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Leichtgewichtiges Partikel- & Effekt-System.
 * Statisch nutzbar: effekt.spawnHit(...), effekt.update(), effekt.draw(gc)
 */
public class effekt {

    private enum Kind { SPARK, RING, DUST, TRAIL, TEXT, FLASH }

    private static final class P {
        Kind kind;
        double x, y, vx, vy;
        double life, maxLife;
        double size;
        Color color;
        String text;

        P(Kind k, double x, double y, double vx, double vy, double life, double size, Color c) {
            this.kind = k; this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.maxLife = life; this.size = size; this.color = c;
        }
    }

    private static final List<P> particles = new ArrayList<>();

    // Globaler Screen-Flash (z.B. bei Strong-Hit oder K.O.)
    private static double flashAlpha = 0;
    private static Color  flashColor = Color.WHITE;

    // ─── Spawning ───────────────────────────────────────────────────────────

    /** Treffer-Funken + Ring-Explosion an Hitbox-Mitte. */
    public static void spawnHit(double cx, double cy, Color color, float dmg) {
        int sparkCount = 6 + (int) (dmg / 3);
        for (int i = 0; i < sparkCount; i++) {
            double ang = Math.random() * Math.PI * 2;
            double spd = 2.5 + Math.random() * 4.5 * (1 + dmg / 30.0);
            particles.add(new P(Kind.SPARK, cx, cy, Math.cos(ang) * spd, Math.sin(ang) * spd,
                    14 + Math.random() * 10, 2 + Math.random() * 2.5, color));
        }
        // Schock-Ring
        particles.add(new P(Kind.RING, cx, cy, 0, 0, 18, 6, color));
        // Treffer-Text "%-Schaden" Pop
        particles.add(new P(Kind.TEXT, cx, cy - 18, 0, -0.6, 26, dmg, color));

        if (dmg >= 14) flash(Color.color(1, 1, 1, 0.18), 8);
    }

    /** Staubwolke beim Landen / Springen. */
    public static void spawnDust(double cx, double groundY, boolean strong) {
        int n = strong ? 10 : 5;
        for (int i = 0; i < n; i++) {
            double ang = Math.PI + Math.random() * Math.PI; // nach oben/außen
            double spd = (strong ? 1.5 : 0.8) + Math.random() * (strong ? 2.5 : 1.5);
            particles.add(new P(Kind.DUST, cx, groundY, Math.cos(ang) * spd, Math.sin(ang) * spd * 0.6,
                    16 + Math.random() * 12, 3 + Math.random() * (strong ? 5 : 3),
                    Color.color(0.8, 0.8, 0.9, 0.35)));
        }
    }

    /** Bewegungs-Trail-Partikel (z.B. bei schnellem Lauf oder Dash). */
    public static void spawnTrail(double cx, double cy, Color color) {
        particles.add(new P(Kind.TRAIL, cx, cy, 0, 0, 10, 16, color));
    }

    /** Kurzer Vollbild-Flash. */
    public static void flash(Color c, double frames) {
        flashAlpha = Math.max(flashAlpha, c.getOpacity());
        flashColor = c;
        flashTimer = frames;
    }

    private static double flashTimer = 0;

    // ─── Update ─────────────────────────────────────────────────────────────

    public static void update() {
        Iterator<P> it = particles.iterator();
        while (it.hasNext()) {
            P p = it.next();
            p.life--;
            if (p.life <= 0) { it.remove(); continue; }

            switch (p.kind) {
                case SPARK -> {
                    p.x += p.vx; p.y += p.vy;
                    p.vx *= 0.92; p.vy *= 0.92;
                    p.vy += 0.15; // leichte Schwerkraft
                }
                case DUST -> {
                    p.x += p.vx; p.y += p.vy;
                    p.vx *= 0.94; p.vy *= 0.94;
                    p.size += 0.15;
                }
                case RING, TEXT -> p.y += p.vy;
                case TRAIL -> {}
                default -> {}
            }
        }

        if (flashTimer > 0) {
            flashTimer--;
            flashAlpha = flashAlpha * 0.82;
            if (flashTimer <= 0) flashAlpha = 0;
        }
    }

    // ─── Draw ───────────────────────────────────────────────────────────────

    public static void draw(GraphicsContext gc) {
        for (P p : particles) {
            double prog = p.life / p.maxLife; // 1 -> 0
            switch (p.kind) {
                case SPARK -> {
                    double a = prog;
                    gc.setFill(Color.color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), a));
                    double s = p.size * prog;
                    gc.fillOval(p.x - s / 2, p.y - s / 2, s, s);
                }
                case RING -> {
                    double a = prog * 0.8;
                    double r = (1 - prog) * 46 + 4;
                    gc.setStroke(Color.color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), a));
                    gc.setLineWidth(3 * prog + 0.5);
                    gc.strokeOval(p.x - r / 2, p.y - r / 2, r, r);
                }
                case DUST -> {
                    double a = prog * 0.4;
                    gc.setFill(Color.color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), a));
                    gc.fillOval(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size);
                }
                case TEXT -> {
                    double a = Math.min(1, prog * 1.6);
                    gc.setFont(javafx.scene.text.Font.font("Consolas",
                            javafx.scene.text.FontWeight.BOLD, 15));
                    gc.setFill(Color.color(1, 1, 1, a));
                    String txt = String.format("%.0f", p.size); // p.size trägt dmg
                    gc.fillText(txt, p.x - txt.length() * 4, p.y);
                }
                default -> {}
            }
        }
    }

    /** Vollbild-Flash separat zeichnen (nach allem anderen, vor HUD). */
    public static void drawFlash(GraphicsContext gc, int w, int h) {
        if (flashAlpha > 0.01) {
            gc.setFill(Color.color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), flashAlpha));
            gc.fillRect(0, 0, w, h);
        }
    }

    public static void clear() {
        particles.clear();
        flashAlpha = 0;
        flashTimer = 0;
    }
}
