package Guys2;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class attacke {

    public enum Type {
        JAB   ("Jab",    55, 35, 18, 25, Color.color(0.39,0.71,1,1)),
        STRONG("Strong", 80, 50, 28, 45, Color.color(1,0.47,0,1)),
        UP_AIR("Up-Air", 46, 60, 22, 35, Color.color(0.71,0.39,1,1));

        public final String label;
        public final int hitW, hitH, duration, cooldown;
        public final Color color;

        Type(String l, int hw, int hh, int dur, int cd, Color c) {
            label=l; hitW=hw; hitH=hh; duration=dur; cooldown=cd; color=c;
        }
    }

    private final character owner;
    private Type  current;
    private int   timer       = 0;
    private int   cooldown    = 0;
    private boolean active    = false;
    private boolean hasHit    = false;

    public attacke(character owner) { this.owner = owner; }

    // ─── Update ─────────────────────────────────────────────────────────────

    public void update() {
        if (cooldown > 0) cooldown--;
        if (active) {
            timer--;
            if (timer <= 0) { active = false; hasHit = false; current = null; }
        }
        // Sync to character for draw
        owner.isAttacking   = active;
        owner.currentAttack = active ? typeIndex() : 0;
        owner.attackTimer   = timer;
        owner.attackDuration= (current != null) ? current.duration : 20;
    }

    private int typeIndex() {
        if (current == null) return 0;
        return switch (current) { case JAB -> 1; case STRONG -> 2; case UP_AIR -> 3; };
    }

    // ─── Try to start ────────────────────────────────────────────────────────

    public boolean tryAttack(int idx) {
        if (active || cooldown > 0) return false;
        current = switch (idx) { case 1 -> Type.JAB; case 2 -> Type.STRONG; default -> Type.UP_AIR; };
        timer   = current.duration;
        cooldown= current.cooldown;
        active  = true;
        hasHit  = false;
        owner.velX *= 0.35;
        return true;
    }

    // ─── Hitbox ──────────────────────────────────────────────────────────────

    public double[] getHitbox() {
        if (!active || current == null) return new double[]{0,0,0,0};
        boolean fr = owner.velX >= 0;
        if (current == Type.UP_AIR) {
            return new double[]{owner.x, owner.y - current.hitH, owner.width, current.hitH};
        }
        double hx = fr ? owner.x + owner.width : owner.x - current.hitW;
        double hy = owner.y + owner.height / 2.0 - current.hitH / 2.0;
        return new double[]{hx, hy, current.hitW, current.hitH};
    }

    // ─── Check hit against target ────────────────────────────────────────────

    public boolean checkHit(character target) {
        if (!active || hasHit || current == null) return false;
        // Active hit frames: middle section of animation
        if (timer > current.duration - 3 || timer < 2) return false;

        double[] ab = getHitbox();
        double[] hb = target.getHitbox();
        if (rectsOverlap(ab, hb)) {
            hasHit = true;
            float dmg = damage();
            target.damage += dmg;
            target.applyKnockback(owner.x + owner.width / 2.0, dmg);
            owner.velX *= 0.4;
            return true;
        }
        return false;
    }

    private boolean rectsOverlap(double[] a, double[] b) {
        return a[0] < b[0]+b[2] && a[0]+a[2] > b[0]
                && a[1] < b[1]+b[3] && a[1]+a[3] > b[1];
    }

    public float damage() {
        if (current == null) return 0;
        return switch (current) { case JAB -> 8f; case STRONG -> 16f; case UP_AIR -> 11f; };
    }

    public boolean isActive() { return active; }

    // ─── Draw ────────────────────────────────────────────────────────────────

    public void draw(GraphicsContext gc) {
        if (!active || current == null) return;
        double[] hb = getHitbox();
        if (hb[2] == 0) return;

        double prog = (double) timer / current.duration;
        Color c = current.color;

        gc.setFill(Color.color(c.getRed(),c.getGreen(),c.getBlue(), 0.22 * prog));
        gc.fillRoundRect(hb[0]-6, hb[1]-6, hb[2]+12, hb[3]+12, 16, 16);

        gc.setFill(Color.color(c.getRed(),c.getGreen(),c.getBlue(), 0.6 * prog));
        gc.fillRoundRect(hb[0], hb[1], hb[2], hb[3], 10, 10);

        gc.setStroke(Color.color(1,1,1, 0.75 * prog));
        gc.setLineWidth(1.8);
        gc.strokeRoundRect(hb[0], hb[1], hb[2], hb[3], 10, 10);

        gc.setFill(Color.color(1,1,1, 0.85 * prog));
        gc.setFont(javafx.scene.text.Font.font("Consolas", 11));
        gc.fillText(current.label, hb[0] + 4, hb[1] - 3);
    }
}