package Guys2;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashSet;
import java.util.Set;

public class multiplayer {

    // ─── Constants ──────────────────────────────────────────────────────────
    private static final int W         = inizialisierung.WIDTH;
    private static final int H         = inizialisierung.HEIGHT;
    private static final int MAX_FALLS = 5;

    // ─── Char select data ───────────────────────────────────────────────────
    private static final String[][] CHARS = {
            {"Volt",  "Schnell · leichte Angriffe"},
            {"Rex",   "Schwer  · starker Knockback"},
            {"Nova",  "Balanciert · 3 Angriffe"}
    };
    private static final Color[] BODY_COLORS = {
            Color.color(0.31,0.71,1),
            Color.color(1,0.39,0.31),
            Color.color(0.47,1,0.59)
    };
    private static final Color[] GLOW_COLORS = {
            Color.color(0.16,0.47,1),
            Color.color(0.78,0.24,0.16),
            Color.color(0.24,0.78,0.35)
    };

    // ─── State ──────────────────────────────────────────────────────────────
    private enum Screen { MENU, CHAR_SELECT, PLAYING, GAME_OVER, CONTROLS }
    private Screen screen = Screen.MENU;

    private final Canvas         canvas;
    private final GraphicsContext gc;
    private final Set<KeyCode>   keys = new HashSet<>();

    private stage     gameStage;
    private character p1, p2;
    private attacke   a1, a2;

    private int    menuSel   = 0;
    private int    p1Sel     = 0, p2Sel = 0;
    private boolean p1Ready  = false, p2Ready = false;
    private String  winner   = "";
    private long    uiFrame  = 0;
    private int     shakeP1  = 0, shakeP2 = 0;

    // ─── Constructor ────────────────────────────────────────────────────────
    public multiplayer(Canvas canvas) {
        this.canvas = canvas;
        this.gc     = canvas.getGraphicsContext2D();
    }

    public void start() {
        new AnimationTimer() {
            private long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = (now - last) / 1_000_000.0; // ms
                last = now;
                int ticks = Math.max(1, Math.min(3, (int)(dt / 14)));
                for (int i = 0; i < ticks; i++) tick();
                render();
            }
        }.start();
    }

    // ─── Game tick ──────────────────────────────────────────────────────────
    private void tick() {
        uiFrame++;
        if (screen != Screen.PLAYING) return;

        gameStage.update();

        // P1 input
        boolean p1l = keys.contains(KeyCode.A),
                p1r = keys.contains(KeyCode.D),
                p1j = keys.contains(KeyCode.W);
        boolean p1a1 = keys.contains(KeyCode.F),
                p1a2 = keys.contains(KeyCode.G),
                p1a3 = keys.contains(KeyCode.H);

        // P2 input
        boolean p2l = keys.contains(KeyCode.LEFT),
                p2r = keys.contains(KeyCode.RIGHT),
                p2j = keys.contains(KeyCode.UP);
        boolean p2a1 = keys.contains(KeyCode.K),
                p2a2 = keys.contains(KeyCode.L),
                p2a3 = keys.contains(KeyCode.J);

        // Attacks
        if (p1a1) a1.tryAttack(1);
        else if (p1a2) a1.tryAttack(2);
        else if (p1a3) a1.tryAttack(3);
        if (p2a1) a2.tryAttack(1);
        else if (p2a2) a2.tryAttack(2);
        else if (p2a3) a2.tryAttack(3);

        a1.update(); a2.update();

        // Hit checks
        if (a1.checkHit(p2)) shakeP2 = 14;
        if (a2.checkHit(p1)) shakeP1 = 14;

        p1.update(p1l, p1r, p1j, gameStage);
        p2.update(p2l, p2r, p2j, gameStage);
        kolission.resolveCharacters(p1, p2);

        checkFall(p1);
        checkFall(p2);

        if (shakeP1 > 0) shakeP1--;
        if (shakeP2 > 0) shakeP2--;

        if (p1.fallCount >= MAX_FALLS) { winner = p2.name; screen = Screen.GAME_OVER; menuSel = 0; }
        else if (p2.fallCount >= MAX_FALLS) { winner = p1.name; screen = Screen.GAME_OVER; menuSel = 0; }
    }

    private void checkFall(character c) {
        if (c.x + c.width < -60 || c.x > W + 60 || c.y > H + 10) {
            c.fallCount++;
            c.damage = 0;
            c.x = W / 2.0 - c.width / 2.0;
            c.y = stage.GROUND_Y - c.height - 80;
            c.velX = 0; c.velY = -4;
            c.knockedBack = false;
            if (c.playerIndex == 1) shakeP1 = 28;
            else shakeP2 = 28;
        }
    }

    // ─── Render ─────────────────────────────────────────────────────────────
    private void render() {
        gc.clearRect(0, 0, W, H);
        switch (screen) {
            case MENU       -> drawMenu();
            case CHAR_SELECT-> drawCharSelect();
            case PLAYING    -> drawPlaying();
            case GAME_OVER  -> drawGameOver();
            case CONTROLS   -> drawControls();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PLAYING
    // ═══════════════════════════════════════════════════════════════════════
    private void drawPlaying() {
        gameStage.draw(gc);
        a1.draw(gc); a2.draw(gc);
        p1.draw(gc); p2.draw(gc);
        drawHUD();
    }

    private void drawHUD() {
        drawPlayerPanel(p1,  18,  H - 108, shakeP1 > 0);
        drawPlayerPanel(p2, W - 298, H - 108, shakeP2 > 0);
        drawLives();
        drawControlHint();
    }

    private void drawPlayerPanel(character p, double px, double py, boolean shake) {
        double ox = shake ? (Math.random()*6-3) : 0;
        double oy = shake ? (Math.random()*4-2) : 0;
        double x = px + ox, y = py + oy;
        int bw = 280, bh = 88;

        gc.setFill(Color.color(0.039,0.031,0.098,0.88));
        gc.fillRoundRect(x, y, bw, bh, 14, 14);
        gc.setStroke(p.bodyColor.deriveColor(0,1,0.7,0.6));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, bw, bh, 14, 14);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
        gc.setFill(p.bodyColor);
        gc.fillText("P" + p.playerIndex + "  " + p.name, x + 14, y + 23);

        double barW = 252;
        gc.setFill(Color.color(0.12,0.10,0.24));
        gc.fillRoundRect(x + 14, y + 32, barW, 15, 7, 7);

        double pct = Math.min(p.damage / 150.0, 1.0);
        if (pct > 0) {
            Color bc = (p.damage < 60)
                    ? Color.color(0.24,0.78,0.39)
                    : (p.damage < 120) ? Color.color(0.90,0.63,0.08) : Color.color(0.86,0.20,0.20);
            LinearGradient bar = new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
                    new Stop(0, bc.brighter()), new Stop(1, bc));
            gc.setFill(bar);
            gc.fillRoundRect(x + 14, y + 32, barW * pct, 15, 7, 7);
        }

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 21));
        gc.setFill(Color.WHITE);
        gc.fillText(String.format("%.0f%%", p.damage), x + 14, y + 72);

        gc.setFont(Font.font("Consolas", 13));
        gc.setFill(Color.color(0.71,0.59,1));
        gc.fillText("Tode: " + p.fallCount + "/" + MAX_FALLS, x + 155, y + 72);
    }

    private void drawLives() {
        double cx = W / 2.0;
        double cy = 18;
        for (int i = 0; i < MAX_FALLS; i++) {
            boolean alive = i < (MAX_FALLS - p1.fallCount);
            gc.setFill(alive ? p1.bodyColor : Color.color(0.20,0.16,0.31));
            gc.fillOval(cx - 158 + i * 26, cy, 18, 18);
            if (alive) { gc.setFill(Color.color(1,1,1,0.28)); gc.fillOval(cx-156+i*26, cy+2, 7,5); }
        }
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        gc.setFill(Color.color(0.59,0.51,0.78));
        gc.fillText("VS", cx - 13, cy + 14);
        for (int i = 0; i < MAX_FALLS; i++) {
            boolean alive = i < (MAX_FALLS - p2.fallCount);
            gc.setFill(alive ? p2.bodyColor : Color.color(0.20,0.16,0.31));
            gc.fillOval(cx + 30 + i * 26, cy, 18, 18);
            if (alive) { gc.setFill(Color.color(1,1,1,0.28)); gc.fillOval(cx+32+i*26, cy+2, 7,5); }
        }
    }

    private void drawControlHint() {
        gc.setFont(Font.font("Consolas", 11));
        gc.setFill(Color.color(0.39,0.39,0.63,0.7));
        gc.fillText("P1: WASD · F Jab · G Strong · H Up-Air", 10, H - 6);
        String s2 = "P2: Pfeile · K Jab · L Strong · J Up-Air";
        gc.fillText(s2, W - s2.length()*6.6 - 10, H - 6);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MENU
    // ═══════════════════════════════════════════════════════════════════════
    private void drawMenu() {
        drawDarkBg();

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 74));
        String title = "TWO GUYS";
        double tw = title.length() * 42.0;

        gc.setFill(Color.color(0.31,0.24,0.78,0.22));
        gc.fillText(title, W/2.0 - tw/2 + 4, 184);

        LinearGradient tg = new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.63,0.47,1)),
                new Stop(1, Color.color(0.31,0.78,1)));
        gc.setFill(tg);
        gc.fillText(title, W/2.0 - tw/2, 180);

        gc.setFont(Font.font("Consolas", 17));
        gc.setFill(Color.color(0.59,0.51,0.78));
        String sub = "2-Spieler Plattform Kampf";
        gc.fillText(sub, W/2.0 - sub.length()*4.8, 216);

        String[] opts = {"SPIELEN", "STEUERUNG", "BEENDEN"};
        for (int i = 0; i < opts.length; i++)
            drawMenuBtn(opts[i], i, menuSel, W/2.0, 310 + i * 80);

        drawCornerDeco();
    }

    private void drawMenuBtn(String text, int idx, int sel, double cx, double cy) {
        boolean active = idx == sel;
        double pulse = 0.7 + 0.3 * Math.sin(uiFrame * 0.08);
        double bw = 280, bh = 52, bx = cx - bw/2;

        if (active) {
            gc.setFill(Color.color(0.31,0.24,0.78, 0.22 * pulse));
            gc.fillRoundRect(bx-8, cy-bh/2-8, bw+16, bh+16, 18, 18);
            LinearGradient bg = new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(0.196,0.137,0.510)),
                    new Stop(1, Color.color(0.118,0.078,0.353)));
            gc.setFill(bg);
            gc.fillRoundRect(bx, cy-bh/2, bw, bh, 11, 11);
            LinearGradient border = new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,
                    new Stop(0,Color.color(0.63,0.47,1)), new Stop(1,Color.color(0.31,0.78,1)));
            gc.setStroke(border); gc.setLineWidth(2.0);
            gc.strokeRoundRect(bx, cy-bh/2, bw, bh, 11, 11);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 23));
            gc.setFill(Color.WHITE);
        } else {
            gc.setFill(Color.color(0.078,0.059,0.196,0.75));
            gc.fillRoundRect(bx, cy-bh/2, bw, bh, 11, 11);
            gc.setStroke(Color.color(0.235,0.196,0.431)); gc.setLineWidth(1.0);
            gc.strokeRoundRect(bx, cy-bh/2, bw, bh, 11, 11);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 21));
            gc.setFill(Color.color(0.55,0.47,0.78));
        }

        double textX = cx - text.length() * (active ? 6.9 : 6.3);
        gc.fillText(text, textX, cy + 8);

        if (active) {
            gc.setFill(Color.color(0.63,0.47,1));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
            gc.fillText("▶", bx - 26, cy + 7);
            gc.fillText("◀", bx + bw + 6, cy + 7);
        }
    }

    private void drawCornerDeco() {
        gc.setStroke(Color.color(0.31,0.24,0.71,0.45)); gc.setLineWidth(1.5);
        int m = 20;
        gc.strokeLine(m, m, m+50, m); gc.strokeLine(m, m, m, m+50);
        gc.strokeLine(W-m-50, m, W-m, m); gc.strokeLine(W-m, m, W-m, m+50);
        gc.strokeLine(m, H-m, m+50, H-m); gc.strokeLine(m, H-m-50, m, H-m);
        gc.strokeLine(W-m-50, H-m, W-m, H-m); gc.strokeLine(W-m, H-m-50, W-m, H-m);
    }

    private void drawDarkBg() {
        LinearGradient sky = new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.020,0.016,0.059)),
                new Stop(1, Color.color(0.047,0.031,0.137)));
        gc.setFill(sky); gc.fillRect(0,0,W,H);
        gc.setStroke(Color.color(0.235,0.157,0.471,0.15)); gc.setLineWidth(0.5);
        for (int x=0;x<W;x+=60) gc.strokeLine(x,0,x,H);
        for (int y=0;y<H;y+=60) gc.strokeLine(0,y,W,y);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTROLS
    // ═══════════════════════════════════════════════════════════════════════
    private void drawControls() {
        drawDarkBg();

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 54));
        gc.setFill(Color.color(0.71,0.59,1));
        String t = "STEUERUNG";
        gc.fillText(t, W/2.0 - t.length()*15.3, 110);

        int boxY = 170;

        // --- P1 Box ---
        gc.setFill(Color.color(0.059,0.047,0.157,0.75));
        gc.fillRoundRect(100, boxY, 450, 320, 16, 16);
        gc.setStroke(BODY_COLORS[0]);
        gc.setLineWidth(2);
        gc.strokeRoundRect(100, boxY, 450, 320, 16, 16);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
        gc.setFill(BODY_COLORS[0]);
        gc.fillText("SPIELER 1", 130, boxY + 45);

        gc.setFont(Font.font("Consolas", 18));
        gc.setFill(Color.WHITE);
        gc.fillText("W / A / S / D  - Bewegen & Springen", 130, boxY + 100);
        gc.fillText("F              - Jab (Schneller Schlag)", 130, boxY + 140);
        gc.fillText("G              - Strong (Starker Schlag)", 130, boxY + 180);
        gc.fillText("H              - Up-Air (Angriff nach oben)", 130, boxY + 220);

        // --- P2 Box ---
        gc.setFill(Color.color(0.059,0.047,0.157,0.75));
        gc.fillRoundRect(W - 550, boxY, 450, 320, 16, 16);
        gc.setStroke(BODY_COLORS[1]);
        gc.setLineWidth(2);
        gc.strokeRoundRect(W - 550, boxY, 450, 320, 16, 16);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
        gc.setFill(BODY_COLORS[1]);
        gc.fillText("SPIELER 2", W - 520, boxY + 45);

        gc.setFont(Font.font("Consolas", 18));
        gc.setFill(Color.WHITE);
        gc.fillText("Pfeiltasten    - Bewegen & Springen", W - 520, boxY + 100);
        gc.fillText("K              - Jab (Schneller Schlag)", W - 520, boxY + 140);
        gc.fillText("L              - Strong (Starker Schlag)", W - 520, boxY + 180);
        gc.fillText("J              - Up-Air (Angriff nach oben)", W - 520, boxY + 220);

        // --- Zurück Info ---
        gc.setFont(Font.font("Consolas", 16));
        gc.setFill(Color.color(0.59,0.51,0.78));
        String esc = "Drücke ESC oder ENTER, um ins Menü zurückzukehren";
        gc.fillText(esc, W/2.0 - esc.length()*4.5, H - 60);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHAR SELECT
    // ═══════════════════════════════════════════════════════════════════════
    private void drawCharSelect() {
        drawDarkBg();

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 34));
        gc.setFill(Color.color(0.71,0.59,1));
        String t = "CHARAKTER AUSWAHL";
        gc.fillText(t, W/2.0 - t.length()*10.2, 68);

        drawCharPanel(60,  110, p1Sel, p1Ready, 1);
        drawCharPanel(W/2.0+60, 110, p2Sel, p2Ready, 2);

        gc.setStroke(Color.color(0.31,0.24,0.71,0.5)); gc.setLineWidth(1.5);
        gc.strokeLine(W/2.0, 90, W/2.0, H-70);

        gc.setFont(Font.font("Consolas", 15));
        if (p1Ready && p2Ready) {
            gc.setFill(Color.color(0.31,0.86,0.47));
            String s = "Beide bereit! Spiel startet...";
            gc.fillText(s, W/2.0 - s.length()*4.5, H-28);
        } else {
            gc.setFill(Color.color(0.59,0.51,0.78));
            gc.fillText("P1: W/S navigieren  ·  ENTER bestätigen", 70, H-28);
            gc.fillText("P2: Pfeile navigieren  ·  LEERTASTE bestätigen", W/2.0+70, H-28);
        }
    }

    private void drawCharPanel(double px, double py, int sel, boolean ready, int pNum) {
        Color pc = BODY_COLORS[pNum-1];
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        gc.setFill(pc);
        gc.fillText("SPIELER " + pNum, px, py);

        for (int i = 0; i < CHARS.length; i++) {
            boolean isSel = (i == sel);
            double cx = px, cy = py + 46 + i*98, cw = 370, ch = 78;

            gc.setFill(isSel ? Color.color(0.157,0.118,0.392,0.85) : Color.color(0.059,0.047,0.157,0.75));
            gc.fillRoundRect(cx, cy, cw, ch, 12, 12);
            gc.setStroke(isSel ? BODY_COLORS[i] : Color.color(0.196,0.157,0.353));
            gc.setLineWidth(isSel ? 2.0 : 1.0);
            gc.strokeRoundRect(cx, cy, cw, ch, 12, 12);

            Color mc = BODY_COLORS[i];
            Color ml = mc.deriveColor(0, 0.55, 1.55, 1);
            gc.setFill(ml);
            gc.fillOval(cx+14, cy+14, 40, 40);
            gc.fillOval(cx+11, cy+18, 9, 9);
            gc.fillOval(cx+46, cy+18, 9, 9);
            gc.setFill(Color.color(1,0.71,0.78, 0.60));
            gc.fillOval(cx+13, cy+20, 5, 5);
            gc.fillOval(cx+48, cy+20, 5, 5);
            gc.setFill(Color.WHITE);
            gc.fillOval(cx+19, cy+22, 12, 13);
            gc.fillOval(cx+36, cy+22, 12, 13);
            gc.setFill(Color.color(0.08,0.06,0.14));
            gc.fillOval(cx+21, cy+24, 8, 9);
            gc.fillOval(cx+38, cy+24, 8, 9);
            gc.setFill(mc.deriveColor(0,0.8,1.1,0.9));
            gc.fillOval(cx+22, cy+25, 6, 6);
            gc.fillOval(cx+39, cy+25, 6, 6);
            gc.setFill(Color.WHITE);
            gc.fillOval(cx+23, cy+25, 3, 3);
            gc.fillOval(cx+40, cy+25, 3, 3);
            gc.fillOval(cx+26, cy+30, 1.5, 1.5);
            gc.fillOval(cx+43, cy+30, 1.5, 1.5);
            gc.setFill(Color.color(1,0.55,0.65, 0.35));
            gc.fillOval(cx+15, cy+33, 9, 5);
            gc.fillOval(cx+43, cy+33, 9, 5);
            gc.setStroke(Color.color(0.3,0.1,0.1,0.45));
            gc.setLineWidth(1.5);
            gc.strokeArc(cx+22, cy+34, 14, 6, 200, 140, javafx.scene.shape.ArcType.OPEN);

            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
            gc.setFill(isSel ? Color.WHITE : Color.color(0.55,0.47,0.71));
            gc.fillText(CHARS[i][0], cx+64, cy+30);
            gc.setFont(Font.font("Consolas", 12));
            gc.setFill(Color.color(0.51,0.47,0.67));
            gc.fillText(CHARS[i][1], cx+64, cy+50);

            if (isSel && !ready) {
                gc.setFill(BODY_COLORS[i]);
                gc.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
                gc.fillText("◀", cx-22, cy+46);
            }
        }

        if (ready) {
            gc.setFill(Color.color(0.31,0.86,0.47,0.18));
            gc.fillRoundRect(px, py+46, 370, 298, 12, 12);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 28));
            gc.setFill(Color.color(0.31,0.94,0.51));
            gc.fillText("BEREIT!", px+120, py+206);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GAME OVER
    // ═══════════════════════════════════════════════════════════════════════
    private void drawGameOver() {
        if (gameStage != null) { gameStage.draw(gc); p1.draw(gc); p2.draw(gc); }

        gc.setFill(Color.color(0,0,0,0.72));
        gc.fillRect(0,0,W,H);

        double bx = W/2.0-300, by = H/2.0-190;
        gc.setFill(Color.color(0.047,0.031,0.137,0.95));
        gc.fillRoundRect(bx, by, 600, 380, 22, 22);
        LinearGradient border = new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
                new Stop(0,Color.color(0.63,0.47,1)), new Stop(1,Color.color(0.31,0.78,1)));
        gc.setStroke(border); gc.setLineWidth(2.5);
        gc.strokeRoundRect(bx, by, 600, 380, 22, 22);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 54));
        LinearGradient goGrad = new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                new Stop(0,Color.color(1,0.31,0.31)), new Stop(1,Color.color(0.86,0.16,0.16)));
        gc.setFill(goGrad);
        String go = "GAME OVER";
        gc.fillText(go, W/2.0 - go.length()*15.3, by+82);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 27));
        gc.setFill(Color.color(0.71,0.86,1));
        String win = winner + " GEWINNT!";
        gc.fillText(win, W/2.0 - win.length()*8.1, by+138);

        gc.setFont(Font.font("Consolas", 15));
        gc.setFill(Color.color(0.47,0.43,0.67));
        gc.fillText(p1.name + "  Tode: " + p1.fallCount + "   Schaden: " + String.format("%.0f%%",p1.damage), bx+40, by+196);
        gc.fillText(p2.name + "  Tode: " + p2.fallCount + "   Schaden: " + String.format("%.0f%%",p2.damage), bx+40, by+220);

        String[] opts = {"NOCHMAL", "MENÜ"};
        for (int i = 0; i < opts.length; i++)
            drawMenuBtn(opts[i], i, menuSel, W/2.0, by+288+i*68);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INPUT
    // ═══════════════════════════════════════════════════════════════════════
    public void onKeyPressed(KeyEvent e) {
        keys.add(e.getCode());
        switch (screen) {
            case MENU        -> handleMenuKey(e.getCode());
            case CHAR_SELECT -> handleCharKey(e.getCode());
            case GAME_OVER   -> handleGameOverKey(e.getCode());
            case CONTROLS    -> handleControlsKey(e.getCode());
        }
    }

    public void onKeyReleased(KeyEvent e) { keys.remove(e.getCode()); }

    private void handleMenuKey(KeyCode k) {
        if (k == KeyCode.UP   || k == KeyCode.W) menuSel = Math.max(0, menuSel-1);
        if (k == KeyCode.DOWN || k == KeyCode.S) menuSel = Math.min(2, menuSel+1);
        if (k == KeyCode.ENTER || k == KeyCode.SPACE) {
            switch (menuSel) {
                case 0 -> { screen = Screen.CHAR_SELECT; p1Ready=false; p2Ready=false; menuSel=0; }
                case 1 -> { screen = Screen.CONTROLS; }
                case 2 -> javafx.application.Platform.exit();
            }
        }
    }

    private void handleControlsKey(KeyCode k) {
        if (k == KeyCode.ESCAPE || k == KeyCode.ENTER || k == KeyCode.SPACE) {
            screen = Screen.MENU;
        }
    }

    private void handleCharKey(KeyCode k) {
        if (!p1Ready) {
            if (k == KeyCode.W)     p1Sel = Math.max(0, p1Sel - 1);
            if (k == KeyCode.S)     p1Sel = Math.min(CHARS.length - 1, p1Sel + 1);
            if (k == KeyCode.ENTER) p1Ready = true;
        }
        if (!p2Ready) {
            if (k == KeyCode.UP)    p2Sel = Math.max(0, p2Sel - 1);
            if (k == KeyCode.DOWN)  p2Sel = Math.min(CHARS.length - 1, p2Sel + 1);
            if (k == KeyCode.SPACE) p2Ready = true;
        }
        if (p1Ready && p2Ready) startGame();
        if (k == KeyCode.ESCAPE) { screen = Screen.MENU; p1Ready = false; p2Ready = false; menuSel = 0; }
    }

    private void handleGameOverKey(KeyCode k) {
        if (k==KeyCode.UP   || k==KeyCode.W) menuSel = Math.max(0, menuSel-1);
        if (k==KeyCode.DOWN || k==KeyCode.S) menuSel = Math.min(1, menuSel+1);
        if (k==KeyCode.ENTER || k==KeyCode.SPACE) {
            if (menuSel==0) startGame();
            else { screen=Screen.MENU; menuSel=0; }
        }
    }

    private void startGame() {
        gameStage = new stage();
        p1 = new character(300, stage.GROUND_Y-64,
                BODY_COLORS[p1Sel], GLOW_COLORS[p1Sel], CHARS[p1Sel][0], 1);
        p2 = new character(800, stage.GROUND_Y-64,
                BODY_COLORS[p2Sel], GLOW_COLORS[p2Sel], CHARS[p2Sel][0], 2);
        a1 = new attacke(p1);
        a2 = new attacke(p2);
        screen = Screen.PLAYING;
    }
}