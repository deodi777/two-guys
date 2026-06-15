package Guys2;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;

import java.util.ArrayList;
import java.util.List;

public class stage {

    public static final int W = inizialisierung.WIDTH;
    public static final int H = inizialisierung.HEIGHT;
    public static final int GROUND_Y = 560;

    private final List<double[]> solidPlatforms   = new ArrayList<>();
    private final List<double[]> floatingPlatforms = new ArrayList<>();

    private final double[] starX, starY;
    private final int[]    starR;
    private final double[] pX, pY, pAlpha;

    private long frame = 0;

    public stage() {
        // Ground
        solidPlatforms.add(new double[]{150, GROUND_Y, W - 300, H - GROUND_Y});

        // Floating platforms
        floatingPlatforms.add(new double[]{150,  420, 200, 18});
        floatingPlatforms.add(new double[]{500,  350, 200, 18});
        floatingPlatforms.add(new double[]{850,  420, 200, 18});
        floatingPlatforms.add(new double[]{350,  270, 180, 18});
        floatingPlatforms.add(new double[]{670,  270, 180, 18});
        floatingPlatforms.add(new double[]{W/2.0-100, 200, 200, 20});

        // Stars
        starX = new double[130]; starY = new double[130]; starR = new int[130];
        for (int i = 0; i < starX.length; i++) {
            starX[i] = Math.random() * W;
            starY[i] = Math.random() * (GROUND_Y - 40);
            starR[i] = (int)(Math.random() * 2) + 1;
        }
        // Particles
        pX = new double[30]; pY = new double[30]; pAlpha = new double[30];
        for (int i = 0; i < pX.length; i++) {
            pX[i] = Math.random() * W;
            pY[i] = Math.random() * GROUND_Y;
            pAlpha[i] = Math.random();
        }
    }

    public void update() {
        frame++;
        for (int i = 0; i < pX.length; i++) {
            pY[i] -= 0.45;
            pAlpha[i] += 0.007;
            if (pY[i] < 0 || pAlpha[i] > 1.0) {
                pY[i] = GROUND_Y - 10;
                pX[i] = Math.random() * W;
                pAlpha[i] = 0;
            }
        }
    }

    // ─── Collision ──────────────────────────────────────────────────────────

    public void resolveCollision(character c) {
        c.onGround = false;

        for (double[] p : solidPlatforms) {
            if (overlaps(c, p)) {
                double prevBottom = c.y + c.height - c.velY;
                if (prevBottom <= p[1] + 14 && c.velY >= 0) {
                    c.y = p[1] - c.height;
                    c.velY = 0;
                    c.onGround = true;
                    // knockback NICHT abbrechen beim Landen — er soll weiterrutschen
                }
            }
        }
        for (double[] p : floatingPlatforms) {
            if (overlaps(c, p)) {
                double prevBottom = c.y + c.height - c.velY;
                boolean strongKnockback = c.knockedBack && Math.abs(c.velX) > 6;
                if (prevBottom <= p[1] + 6 && c.velY >= 0 && !strongKnockback) {
                    c.y = p[1] - c.height;
                    c.velY = 0;
                    c.onGround = true;
                }
            }
        }
    }

    private boolean overlaps(character c, double[] p) {
        return c.x < p[0] + p[2] && c.x + c.width > p[0]
                && c.y < p[1] + p[3] && c.y + c.height > p[1];
    }

    // ─── Draw ───────────────────────────────────────────────────────────────

    public void draw(GraphicsContext gc) {
        drawBackground(gc);
        drawGround(gc);
        drawFloating(gc);
    }

    private void drawBackground(GraphicsContext gc) {
        LinearGradient sky = new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.031,0.024,0.078)),
                new Stop(1, Color.color(0.071,0.047,0.176)));
        gc.setFill(sky);
        gc.fillRect(0, 0, W, H);

        gc.setStroke(Color.color(0.24,0.16,0.47,0.18));
        gc.setLineWidth(0.6);
        for (int x = 0; x < W; x += 60) gc.strokeLine(x, 0, x, H);
        for (int y = 0; y < H; y += 60) gc.strokeLine(0, y, W, y);

        for (int i = 0; i < starX.length; i++) {
            double twinkle = Math.max(0.0, Math.min(1.0, 0.4 + 0.6 * Math.sin(frame * 0.04 + i * 0.71)));
            gc.setFill(Color.color(1, 1, 1, twinkle));
            gc.fillOval(starX[i], starY[i], starR[i], starR[i]);
        }

        for (int i = 0; i < pX.length; i++) {
            double a = Math.max(0, Math.min(pAlpha[i]*2-1, 1)) * 0.5;
            if (a > 0.02) {
                gc.setFill(Color.color(0.39,0.47,1, a));
                gc.fillOval(pX[i], pY[i], 3, 3);
            }
        }

        LinearGradient fog = new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.118,0.059,0.235,0)),
                new Stop(1, Color.color(0.118,0.059,0.235,0.45)));
        gc.setFill(fog);
        gc.fillRect(0, GROUND_Y - 90, W, 90);
    }

    private void drawGround(GraphicsContext gc) {
        double startX = 150;
        double stageW = W - 300;

        LinearGradient glow = new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.31,0.20,0.71,0.35)),
                new Stop(1, Color.color(0.31,0.20,0.71,0)));
        gc.setFill(glow);
        gc.fillRect(startX, GROUND_Y - 28, stageW, 28);

        LinearGradient body = new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.086,0.063,0.196)),
                new Stop(1, Color.color(0.039,0.031,0.098)));
        gc.setFill(body);
        gc.fillRect(startX, GROUND_Y, stageW, H - GROUND_Y);

        gc.setStroke(Color.color(0.471,0.314,1,0.87));
        gc.setLineWidth(2.5);
        gc.strokeLine(startX, GROUND_Y, startX + stageW, GROUND_Y);

        gc.setStroke(Color.color(0.235,0.157,0.549,0.4));
        gc.setLineWidth(1.2);
        gc.strokeLine(startX, GROUND_Y + 4, startX + stageW, GROUND_Y + 4);

        gc.setStroke(Color.color(0.471,0.314,1,0.87));
        gc.setLineWidth(1.5);
        gc.strokeLine(startX, GROUND_Y, startX, H);
        gc.strokeLine(startX + stageW, GROUND_Y, startX + stageW, H);

        gc.setStroke(Color.color(0.39,0.27,0.78,0.1));
        gc.setLineWidth(0.5);
        for (int x = (int)startX; x <= startX + stageW; x += 60) gc.strokeLine(x, GROUND_Y, x, H);
        for (int y = GROUND_Y; y < H; y += 30) gc.strokeLine(startX, y, startX + stageW, y);
    }

    private void drawFloating(GraphicsContext gc) {
        for (double[] p : floatingPlatforms) {
            double px = p[0], py = p[1], pw = p[2], ph = p[3];

            gc.setFill(Color.color(0.314,0.784,1,0.12));
            gc.fillRoundRect(px - 6, py + ph, pw + 12, 22, 10, 10);

            LinearGradient platGrad = new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(0.157,0.118,0.353)),
                    new Stop(1, Color.color(0.098,0.071,0.255)));
            gc.setFill(platGrad);
            gc.fillRoundRect(px, py, pw, ph, 7, 7);

            gc.setStroke(Color.color(0.392,0.863,1,0.78));
            gc.setLineWidth(2.0);
            gc.strokeLine(px + 5, py + 1.5, px + pw - 5, py + 1.5);

            gc.setStroke(Color.color(0.314,0.627,0.863,0.4));
            gc.setLineWidth(1.0);
            gc.strokeRoundRect(px, py, pw, ph, 7, 7);

            gc.setFill(Color.color(0.588,0.902,1,0.63));
            gc.fillOval(px + 10, py + 5, 5, 5);
            gc.fillOval(px + pw - 15, py + 5, 5, 5);
        }
    }
}