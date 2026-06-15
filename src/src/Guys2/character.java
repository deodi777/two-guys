package Guys2;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class character {

    // ─── Position & Physics ─────────────────────────────────────────────────
    public double x, y;
    public double velX, velY;
    public boolean onGround;

    public final int width  = 46;
    public final int height = 62;

    // ─── Stats ──────────────────────────────────────────────────────────────
    public double damage   = 0;
    public int    fallCount = 0;
    public boolean knockedBack = false;
    public int     knockbackTimer = 0;

    // ─── Attack state (written by attacke, read by draw) ────────────────────
    public boolean isAttacking   = false;
    public int     currentAttack = 0;   // 1=jab 2=strong 3=upair
    public int     attackTimer   = 0;
    public int     attackDuration = 20;

    // ─── Identity ───────────────────────────────────────────────────────────
    public final Color  bodyColor;
    public final Color  glowColor;
    public final String name;
    public final int    playerIndex;

    // ─── Physics constants ──────────────────────────────────────────────────
    private static final double GRAVITY     = 0.55;
    private static final double JUMP_FORCE  = -14.0;
    private static final double SPEED       = 3.5;
    private static final double KB_CONTROL  = 0.30;

    // facing & animation variables
    public boolean facingRight = true;
    private double  walkPhase   = 0;

    public character(double sx, double sy, Color bodyColor, Color glowColor,
                     String name, int playerIndex) {
        this.x = sx; this.y = sy;
        this.bodyColor = bodyColor; this.glowColor = glowColor;
        this.name = name; this.playerIndex = playerIndex;
    }

    // ─── Update ─────────────────────────────────────────────────────────────

    public void update(boolean left, boolean right, boolean jump, stage gameStage) {
        if (knockedBack) { if (--knockbackTimer <= 0) knockedBack = false; }

        double control = knockedBack ? KB_CONTROL : 1.0;
        if (left)  velX -= SPEED * control;
        if (right) velX += SPEED * control;
        velX *= 0.72;
        if (velX >  0.1) facingRight = true;
        if (velX < -0.1) facingRight = false;

        if (jump && onGround) { velY = JUMP_FORCE; onGround = false; }

        velY += GRAVITY;
        if (knockedBack) velX *= 0.90;

        velX = Math.max(-12, Math.min(12, velX));
        velY = Math.max(-18, Math.min(18, velY));

        x += velX;
        y += velY;

        if (onGround && Math.abs(velX) > 0.5) {
            walkPhase += Math.abs(velX) * 0.12;
        } else {
            walkPhase = 0;
        }

        gameStage.resolveCollision(this);
    }

    // ─── Hitbox ──────────────────────────────────────────────────────────────

    public double[] getHitbox() {
        return new double[]{x, y, width, height};
    }

    public double[] getAttackBox() {
        if (!isAttacking) return new double[]{0,0,0,0};
        int aw = (currentAttack == 1) ? 55 : (currentAttack == 3) ? width : 80;
        int ah = (currentAttack == 1) ? 35 : (currentAttack == 3) ? 60  : 50;
        double ax, ay;
        if (currentAttack == 3) {
            ax = x; ay = y - ah;
        } else {
            ax = facingRight ? x + width : x - aw;
            ay = y + height / 2.0 - ah / 2.0;
        }
        return new double[]{ax, ay, aw, ah};
    }

    // ─── Knockback ──────────────────────────────────────────────────────────

    public void applyKnockback(attacke.Type attackType, boolean attackerFacingRight, double dmg) {
        double scale = 1.0 + (damage / 25.0);

        switch (attackType) {
            case JAB -> {
                double dir = attackerFacingRight ? 1 : -1;
                velX = dir * (5.0 + dmg / 35.0) * scale;
                velY = -1.5; // immer minimal, nie skaliert
            }
            case STRONG -> {
                double dir = attackerFacingRight ? 1 : -1;
                velX = dir * (8.0 + dmg / 28.0) * scale;
                velY = -2.0; // immer minimal, nie skaliert
            }
            case UP_AIR -> {
                double dir = attackerFacingRight ? 1 : -1;
                velX = dir * (1.5 + dmg / 60.0) * scale;
                velY = (-7.0 - dmg / 20.0) * scale;
                velY = Math.max(velY, -18.0);
            }
        }

        knockedBack    = true;
        knockbackTimer = 35;
    }

    // ─── Draw ───────────────────────────────────────────────────────────────

    public void draw(GraphicsContext gc) {
        int ix = (int) x, iy = (int) y;

        gc.setFill(Color.color(0,0,0,0.28));
        gc.fillOval(ix + 4, iy + height - 5, width - 8, 10);

        if (knockedBack) {
            RadialGradient rg = new RadialGradient(0,0,0.5,0.5,0.5,true,CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(1,0.2,0.2,0.35)),
                    new Stop(1, Color.color(1,0.2,0.2,0)));
            gc.setFill(rg);
            gc.fillOval(ix - 18, iy - 10, width + 36, height + 20);
        }

        drawBody(gc, ix, iy);

        if (isAttacking) {
            double[] ab = getAttackBox();
            if (ab[2] > 0) {
                Color ac = switch (currentAttack) {
                    case 1  -> glowColor.deriveColor(0,1,1.3,0.65);
                    case 3  -> Color.color(0.7,0.39,1,0.7);
                    default -> Color.color(1,0.47,0,0.72);
                };
                double prog = (double) attackTimer / attackDuration;
                gc.setFill(Color.color(ac.getRed(),ac.getGreen(),ac.getBlue(), ac.getOpacity()*prog));
                gc.fillRoundRect(ab[0], ab[1], ab[2], ab[3], 12, 12);
                gc.setStroke(Color.color(1,1,1, 0.55*prog));
                gc.setLineWidth(1.8);
                gc.strokeRoundRect(ab[0], ab[1], ab[2], ab[3], 12, 12);
            }
        }

        drawHUD(gc, ix, iy);
    }

    private void drawBody(GraphicsContext gc, int ix, int iy) {
        Color base  = bodyColor;
        Color dark  = bodyColor.deriveColor(0, 0.85, 0.60, 1);
        Color light = bodyColor.deriveColor(0, 0.55, 1.55, 1);
        Color skin  = Color.color(1.0, 0.90, 0.82, 1.0);
        Color skinDark = Color.color(0.95, 0.78, 0.68, 1.0);

        double legOffset1 = 0;
        double legOffset2 = 0;
        double bodyBob    = 0;

        if (onGround && Math.abs(velX) > 0.5) {
            legOffset1 = Math.sin(walkPhase) * 7;
            legOffset2 = Math.sin(walkPhase + Math.PI) * 7;
            bodyBob    = Math.abs(Math.sin(walkPhase)) * 3;
        } else if (!onGround) {
            legOffset1 = -3;
            legOffset2 = 4;
        }

        int bodyY = iy - (int)bodyBob;

        gc.setFill(dark);
        gc.fillRoundRect(ix + 9 + legOffset1,  iy + 44, 11, 18, 8, 8);
        gc.fillRoundRect(ix + 26 + legOffset2, iy + 44, 11, 18, 8, 8);
        Color shoeColor = base.deriveColor(0, 0.6, 0.45, 1);
        gc.setFill(shoeColor);
        gc.fillRoundRect(ix + 7 + legOffset1,  iy + 56, 15, 9, 6, 6);
        gc.fillRoundRect(ix + 24 + legOffset2, iy + 56, 15, 9, 6, 6);

        LinearGradient torsoGrad = new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                new Stop(0, light), new Stop(1, dark));
        gc.setFill(torsoGrad);
        gc.fillRoundRect(ix + 3, bodyY + 22, width - 6, 28, 14, 14);

        gc.setFill(Color.color(1,1,1, 0.18));
        gc.fillOval(ix + 12, bodyY + 26, 22, 14);

        gc.setFill(dark);
        if (isAttacking) {
            double attackOffset = Math.sin((double) attackTimer / attackDuration * Math.PI) * 22;

            if (currentAttack == 3) {
                gc.fillRoundRect(ix + 6, bodyY + 12 - attackOffset, 11, 16, 8, 8);
                gc.fillRoundRect(ix + width - 17, bodyY + 12 - attackOffset, 11, 16, 8, 8);
                gc.setFill(light);
                gc.fillOval(ix + 5, bodyY + 2 - attackOffset, 13, 13);
                gc.fillOval(ix + width - 18, bodyY + 2 - attackOffset, 13, 13);
            } else {
                double armX = facingRight ? ix + width - 10 + attackOffset : ix - 8 - attackOffset;
                gc.fillRoundRect(armX, bodyY + 25, 18, 11, 8, 8);
                gc.setFill(light);
                gc.fillOval(facingRight ? armX + 12 : armX - 8, bodyY + 23, 13, 13);
                gc.setFill(dark);
                gc.fillRoundRect(facingRight ? ix - 5 : ix + width - 7, bodyY + 25, 12, 11, 8, 8);
            }
        } else {
            gc.fillRoundRect(ix - 6,         bodyY + 25, 12, 11, 8, 8);
            gc.fillRoundRect(ix + width - 6, bodyY + 25, 12, 11, 8, 8);
        }

        gc.setFill(Color.color(0,0,0,0.10));
        gc.fillOval(ix + 5, bodyY + 5, width - 8, 30);
        gc.setFill(light);
        gc.fillOval(ix + 2, bodyY - 2, width - 4, 32);

        gc.setFill(light);
        gc.fillOval(ix - 2, bodyY + 4, 10, 10);
        gc.fillOval(ix + width - 8, bodyY + 4, 10, 10);
        gc.setFill(Color.color(1, 0.71, 0.78, 0.70));
        gc.fillOval(ix, bodyY + 6, 6, 6);
        gc.fillOval(ix + width - 6, bodyY + 6, 6, 6);

        double eyeOffX = facingRight ? 0 : 1;
        gc.setFill(Color.WHITE);
        gc.fillOval(ix + 9  + eyeOffX, bodyY + 5,  13, 14);
        gc.fillOval(ix + 24 + eyeOffX, bodyY + 5,  13, 14);
        gc.setFill(Color.color(0.08, 0.06, 0.14));
        gc.fillOval(ix + 11 + eyeOffX, bodyY + 7,  9, 10);
        gc.fillOval(ix + 26 + eyeOffX, bodyY + 7,  9, 10);
        gc.setFill(bodyColor.deriveColor(0, 0.8, 1.1, 0.85));
        gc.fillOval(ix + 12 + eyeOffX, bodyY + 8,  7, 7);
        gc.fillOval(ix + 27 + eyeOffX, bodyY + 8,  7, 7);
        gc.setFill(Color.WHITE);
        gc.fillOval(ix + 13 + eyeOffX, bodyY + 8,  4, 4);
        gc.fillOval(ix + 28 + eyeOffX, bodyY + 8,  4, 4);
        gc.fillOval(ix + 17 + eyeOffX, bodyY + 13, 2, 2);
        gc.fillOval(ix + 32 + eyeOffX, bodyY + 13, 2, 2);

        gc.setFill(Color.color(1, 0.55, 0.65, 0.35));
        gc.fillOval(ix + 3,  bodyY + 16, 10, 6);
        gc.fillOval(ix + 33, bodyY + 16, 10, 6);

        gc.setStroke(Color.color(0.3, 0.1, 0.1, 0.55));
        gc.setLineWidth(1.8);
        gc.strokeArc(ix + 14, bodyY + 16, 18, 8, 200, 140, javafx.scene.shape.ArcType.OPEN);

        if (knockedBack) {
            gc.setFill(Color.color(1, 0.1, 0.1, 0.20));
            gc.fillRoundRect(ix + 2, bodyY - 2, width - 2, height, 12, 12);
        }

        gc.setStroke(Color.color(0, 0, 0, 0.22));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(ix + 3, bodyY + 22, width - 6, 28, 14, 14);
        gc.strokeOval(ix + 2, bodyY - 2, width - 4, 32);
    }

    private void drawHUD(GraphicsContext gc, int ix, int iy) {
        String txt = String.format("%.0f%%", damage);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        double tw = txt.length() * 8.5;
        double px = ix + width / 2.0 - tw / 2 - 6;
        double py = iy - 38;

        Color pill;
        if      (damage < 50)  pill = Color.color(0.12,0.78,0.39,0.85);
        else if (damage < 100) pill = Color.color(0.90,0.63,0.08,0.85);
        else                   pill = Color.color(0.86,0.20,0.20,0.85);

        gc.setFill(pill);
        gc.fillRoundRect(px, py, tw + 12, 20, 10, 10);
        gc.setFill(Color.WHITE);
        gc.fillText(txt, px + 6, py + 14);
    }
}