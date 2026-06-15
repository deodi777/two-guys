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
    public double damage   = 0;     // damage %
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

    // ─── Physics & Animation constants ──────────────────────────────────────
    private static final double GRAVITY     = 0.55;
    private static final double JUMP_FORCE  = -14.0;
    private static final double SPEED       = 3.5;
    private static final double KB_CONTROL  = 0.30;

    // facing & animation
    private boolean facingRight = true;
    private double  walkPhase   = 0;

    // ─── Squash & Stretch / Landing-Animation ────────────────────────────────
    private double squashX = 1.0, squashY = 1.0;
    private boolean wasOnGround = true;
    private double  landSquat   = 0; // 1 -> 0, klingt ab
    private double  jumpStretch = 0; // 1 -> 0, klingt ab
    private double  prevVelY    = 0;

    // ─── Hit-Reaction ─────────────────────────────────────────────────────────
    private double hitFlash = 0; // 1 -> 0 weißer Aufblitz beim Treffer
    private double prevDamage = 0;

    public character(double sx, double sy, Color bodyColor, Color glowColor,
                     String name, int playerIndex) {
        this.x = sx; this.y = sy;
        this.bodyColor = bodyColor; this.glowColor = glowColor;
        this.name = name; this.playerIndex = playerIndex;
        this.prevDamage = damage;
    }

    // ─── Update ─────────────────────────────────────────────────────────────

    public void update(boolean left, boolean right, boolean jump, stage gameStage) {
        // Treffer-Erkennung (Schaden seit letztem Frame gestiegen?)
        if (damage > prevDamage) {
            hitFlash = 1.0;
        }
        prevDamage = damage;

        // Knockback decay
        if (knockedBack) { if (--knockbackTimer <= 0) knockedBack = false; }

        double control = knockedBack ? KB_CONTROL : 1.0;
        if (left)  velX -= SPEED * control;
        if (right) velX += SPEED * control;
        velX *= 0.72;
        if (velX >  0.1) facingRight = true;
        if (velX < -0.1) facingRight = false;

        boolean jumpStarted = false;
        if (jump && onGround) { velY = JUMP_FORCE; onGround = false; jumpStarted = true; }

        // Gravity
        prevVelY = velY;
        velY += GRAVITY;
        if (knockedBack) velX *= 0.90;

        // Clamp
        velX = Math.max(-12, Math.min(12, velX));
        velY = Math.max(-16, Math.min(18, velY));

        x += velX;
        y += velY;

        // Animation update
        if (onGround && Math.abs(velX) > 0.5) {
            walkPhase += Math.abs(velX) * 0.12;
        } else {
            walkPhase = 0;
        }

        boolean groundBefore = onGround;
        gameStage.resolveCollision(this);

        // ── Landing detection: war in der Luft, ist jetzt am Boden ───────────
        if (!groundBefore && onGround) {
            double impact = Math.min(1.0, Math.abs(prevVelY) / 16.0);
            landSquat = Math.max(0.25, impact); // Stauchung proportional zum Aufprall
            effekt.spawnDust(x + width / 2.0, y + height, impact > 0.45);
        }

        // ── Sprung-Streckung beim Abspringen ─────────────────────────────────
        if (jumpStarted) {
            jumpStretch = 1.0;
            effekt.spawnDust(x + width / 2.0, y + height, false);
        }

        // ── Squash/Stretch sanft ausklingen lassen ───────────────────────────
        landSquat   *= 0.82; if (landSquat   < 0.01) landSquat   = 0;
        jumpStretch *= 0.85; if (jumpStretch < 0.01) jumpStretch = 0;
        hitFlash    *= 0.80; if (hitFlash    < 0.02) hitFlash    = 0;

        // Ziel-Squash berechnen: Landung quetscht breit/flach, Sprung streckt schmal/hoch
        double targetSqX = 1.0 + landSquat * 0.22 - jumpStretch * 0.12;
        double targetSqY = 1.0 - landSquat * 0.28 + jumpStretch * 0.20;
        // sanftes Einschwingen
        squashX += (targetSqX - squashX) * 0.35;
        squashY += (targetSqY - squashY) * 0.35;

        // ── Knockback-Trail-Partikel ─────────────────────────────────────────
        if (knockedBack && Math.abs(velX) > 1.5) {
            effekt.spawnTrail(x + width / 2.0 - Math.signum(velX) * 10, y + height / 2.0,
                    Color.color(1, 0.25, 0.25, 0.5));
        }

        wasOnGround = onGround;
    }

    // ─── Hitbox / Attackbox ──────────────────────────────────────────────────

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

    public void applyKnockback(double srcX, double dmg) {
        double dir  = (x < srcX) ? -1 : 1;
        double base = 5.5 + dmg / 40.0;
        double scale= 1 + damage / 90.0;
        velX = dir * base * scale;
        velY = -4 - dmg / 30.0;
        knockedBack    = true;
        knockbackTimer = 30;

        // Bei kräftigem Treffer leicht zusammendrücken (Reaktion)
        landSquat = Math.max(landSquat, 0.4);
    }

    // ─── Draw ───────────────────────────────────────────────────────────────

    public void draw(GraphicsContext gc) {
        int ix = (int) x, iy = (int) y;

        // Shadow — Größe reagiert leicht auf Sprunghöhe/Squash
        double shadowScale = 1.0 - Math.min(0.35, Math.abs(velY) * 0.012);
        double shW = (width - 8) * shadowScale;
        gc.setFill(Color.color(0,0,0,0.28));
        gc.fillOval(ix + 4 + (width-8-shW)/2.0, iy + height - 5, shW, 10);

        // Knockback glow
        if (knockedBack) {
            RadialGradient rg = new RadialGradient(0,0,0.5,0.5,0.5,true,CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(1,0.2,0.2,0.35)),
                    new Stop(1, Color.color(1,0.2,0.2,0)));
            gc.setFill(rg);
            gc.fillOval(ix - 18, iy - 10, width + 36, height + 20);
        }

        // ── Squash & Stretch Transform um den Charakter-Mittelpunkt-unten ────
        double pivotX = ix + width / 2.0;
        double pivotY = iy + height; // Boden-Bezugspunkt

        gc.save();
        gc.translate(pivotX, pivotY);
        gc.scale(squashX, squashY);
        gc.translate(-pivotX, -pivotY);

        drawBody(gc, ix, iy);

        // Attack box
        if (isAttacking) {
            double[] ab = getAttackBox();
            if (ab[2] > 0) {
                Color ac = switch (currentAttack) {
                    case 1  -> glowColor.deriveColor(0,1,1.3,0.65);
                    case 3  -> Color.color(0.7,0.39,1,0.7);
                    default -> Color.color(1,0.47,0,0.72);
                };
                double prog = (double) attackTimer / attackDuration;
                double pulse = 0.85 + 0.15 * Math.sin(attackTimer * 1.4);
                gc.setFill(Color.color(ac.getRed(),ac.getGreen(),ac.getBlue(), ac.getOpacity()*prog*pulse));
                gc.fillRoundRect(ab[0], ab[1], ab[2], ab[3], 12, 12);
                gc.setStroke(Color.color(1,1,1, 0.55*prog));
                gc.setLineWidth(1.8);
                gc.strokeRoundRect(ab[0], ab[1], ab[2], ab[3], 12, 12);
            }
        }

        // ── Treffer-Weißblitz über dem Körper ────────────────────────────────
        if (hitFlash > 0.01) {
            gc.setFill(Color.color(1,1,1, hitFlash * 0.55));
            gc.fillRoundRect(ix - 4, iy - 6, width + 8, height + 6, 16, 16);
        }

        gc.restore();

        drawHUD(gc, ix, iy);
    }

    private void drawBody(GraphicsContext gc, int ix, int iy) {
        Color base  = bodyColor;
        Color dark  = bodyColor.deriveColor(0, 0.85, 0.60, 1);
        Color light = bodyColor.deriveColor(0, 0.55, 1.55, 1);
        Color skin  = Color.color(1.0, 0.90, 0.82, 1.0);
        Color skinDark = Color.color(0.95, 0.78, 0.68, 1.0);



        // Lauf-Animation Berechnungen
        double legOffset1 = 0;
        double legOffset2 = 0;
        double bodyBob    = 0;
        double leanX      = 0;
        double squishLeg  = 0;

        if (onGround && Math.abs(velX) > 0.5) {
            legOffset1 = Math.sin(walkPhase) * 7;
            legOffset2 = Math.sin(walkPhase + Math.PI) * 7;
            bodyBob    = Math.abs(Math.sin(walkPhase)) * 2;
            // Leichte Lauf-Neigung in Bewegungsrichtung
            leanX = Math.signum(velX) * Math.min(3, Math.abs(velX) * 0.6);
        } else if (!onGround) {
            // Sprung-Pose: Beine ziehen sich an beim Steigen, strecken sich beim Fallen
            if (velY < -1) { legOffset1 = -4; legOffset2 = 5; squishLeg = -2; }
            else if (velY > 4) { legOffset1 = 3; legOffset2 = -3; squishLeg = 1; }
            else { legOffset1 = -3; legOffset2 = 4; }
        }

        // Körper hebt sich leicht beim Laufen
        int bodyY = iy - (int)bodyBob;

        gc.save();
        // Leichte Neigung in Laufrichtung (um Kopf-Pivot)
        if (leanX != 0) {
            gc.translate(ix + width/2.0, bodyY + 10);
            gc.rotate(leanX * 1.5);
            gc.translate(-(ix + width/2.0), -(bodyY + 10));
        }

        // ── Cute stubby legs (Animated) ─────────────────────────────────────
        gc.setFill(dark);
        gc.fillRoundRect(ix + 9 + legOffset1,  iy + 44 + squishLeg, 11, 18 - squishLeg, 8, 8);
        gc.fillRoundRect(ix + 26 + legOffset2, iy + 44 + squishLeg, 11, 18 - squishLeg, 8, 8);
        // Tiny shoes
        Color shoeColor = base.deriveColor(0, 0.6, 0.45, 1);
        gc.setFill(shoeColor);
        gc.fillRoundRect(ix + 7 + legOffset1,  iy + 56, 15, 9, 6, 6);
        gc.fillRoundRect(ix + 24 + legOffset2, iy + 56, 15, 9, 6, 6);



        // ── Chubby round torso ──────────────────────────────────────────────
        LinearGradient torsoGrad = new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                new Stop(0, light), new Stop(1, dark));
        gc.setFill(torsoGrad);
        gc.fillRoundRect(ix + 3, bodyY + 22, width - 6, 28, 14, 14);

        // Belly button / tummy highlight
        gc.setFill(Color.color(1,1,1, 0.18));
        gc.fillOval(ix + 12, bodyY + 26, 22, 14);

        // ── Tiny round arms ─────────────────────────────────────────────────
        gc.setFill(dark);
        if (isAttacking) {
            // Punching arm stretched forward
            double armX = facingRight ? ix + width - 2 : ix - 16;
            gc.fillRoundRect(armX, bodyY + 25, 18, 11, 8, 8);
            // Fist
            gc.setFill(light);
            gc.fillOval(facingRight ? ix + width + 10 : ix - 18, bodyY + 23, 13, 13);
        } else {
            // Leichtes Armschwingen passend zur Laufanimation
            double armSwing1 = onGround && Math.abs(velX) > 0.5 ? Math.sin(walkPhase + Math.PI) * 3 : 0;
            double armSwing2 = onGround && Math.abs(velX) > 0.5 ? Math.sin(walkPhase) * 3 : 0;
            gc.fillRoundRect(ix - 6,         bodyY + 25 + armSwing1, 12, 11, 8, 8);
            gc.fillRoundRect(ix + width - 6, bodyY + 25 + armSwing2, 12, 11, 8, 8);
        }

        // ── Big round cute head ─────────────────────────────────────────────
        // Head shadow
        gc.setFill(Color.color(0,0,0,0.10));
        gc.fillOval(ix + 5, bodyY + 5, width - 8, 30);
        // Head fill
        gc.setFill(light);
        gc.fillOval(ix + 2, bodyY - 2, width - 4, 32);

        // ── Cute ears (little round bumps) ──────────────────────────────────
        gc.setFill(light);
        gc.fillOval(ix - 2, bodyY + 4, 10, 10);
        gc.fillOval(ix + width - 8, bodyY + 4, 10, 10);
        // Inner ear
        gc.setFill(Color.color(1, 0.71, 0.78, 0.70));
        gc.fillOval(ix, bodyY + 6, 6, 6);
        gc.fillOval(ix + width - 6, bodyY + 6, 6, 6);

        // ── Big sparkly eyes ────────────────────────────────────────────────
        double eyeOffX = facingRight ? 0 : 1;

        // Blinzeln: alle ~140 Frames kurzes Augenzwinkern
        boolean blink = (walkBlinkPhase() < 6);

        // Eye whites
        gc.setFill(Color.WHITE);
        double eyeH = blink ? 3 : 14;
        double eyeYAdj = blink ? bodyY + 5 + (14-3)/2.0 : bodyY + 5;
        gc.fillOval(ix + 9  + eyeOffX, eyeYAdj,  13, eyeH);
        gc.fillOval(ix + 24 + eyeOffX, eyeYAdj,  13, eyeH);

        if (!blink) {
            // Pupils (dark)
            gc.setFill(Color.color(0.08, 0.06, 0.14));
            gc.fillOval(ix + 11 + eyeOffX, bodyY + 7,  9, 10);
            gc.fillOval(ix + 26 + eyeOffX, bodyY + 7,  9, 10);
            // Colored iris
            gc.setFill(bodyColor.deriveColor(0, 0.8, 1.1, 0.85));
            gc.fillOval(ix + 12 + eyeOffX, bodyY + 8,  7, 7);
            gc.fillOval(ix + 27 + eyeOffX, bodyY + 8,  7, 7);
            // Big white sparkle highlight
            gc.setFill(Color.WHITE);
            gc.fillOval(ix + 13 + eyeOffX, bodyY + 8,  4, 4);
            gc.fillOval(ix + 28 + eyeOffX, bodyY + 8,  4, 4);
            // Small extra sparkle
            gc.fillOval(ix + 17 + eyeOffX, bodyY + 13, 2, 2);
            gc.fillOval(ix + 32 + eyeOffX, bodyY + 13, 2, 2);
        }

        // ── Cute blush cheeks ────────────────────────────────────────────────
        gc.setFill(Color.color(1, 0.55, 0.65, 0.35));
        gc.fillOval(ix + 3,  bodyY + 16, 10, 6);
        gc.fillOval(ix + 33, bodyY + 16, 10, 6);

        // ── Small smile / Grimasse bei Treffer ──────────────────────────────
        gc.setStroke(Color.color(0.3, 0.1, 0.1, 0.55));
        gc.setLineWidth(1.8);
        if (knockedBack) {
            // schmerzverzogener "O"-Mund
            gc.strokeOval(ix + 19, bodyY + 16, 8, 7);
        } else {
            gc.strokeArc(ix + 14, bodyY + 16, 18, 8, 200, 140, javafx.scene.shape.ArcType.OPEN);
        }

        // ── Knockback red tint ───────────────────────────────────────────────
        if (knockedBack) {
            gc.setFill(Color.color(1, 0.1, 0.1, 0.20));
            gc.fillRoundRect(ix + 2, iy - 2, width - 2, height, 12, 12);
        }

        // ── Outline ──────────────────────────────────────────────────────────
        gc.setStroke(Color.color(0, 0, 0, 0.22));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(ix + 3, bodyY + 22, width - 6, 28, 14, 14);
        gc.strokeOval(ix + 2, bodyY - 2, width - 4, 32);

        gc.restore();
    }

    // Einfache deterministische "Zufalls"-Blinzelphase basierend auf Position+Zeit
    private long blinkSeed = (long)(Math.random() * 200);
    private double walkBlinkPhase() {
        long t = (System.nanoTime() / 16_000_000L + blinkSeed) % 200;
        return t;
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
