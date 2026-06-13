package Guys2;

public class kolission {

    public static void resolveCharacters(character c1, character c2) {
        double[] b1 = c1.getHitbox();
        double[] b2 = c2.getHitbox();

        if (!overlaps(b1, b2)) return;

        double overlapL = (b1[0] + b1[2]) - b2[0];
        double overlapR = (b2[0] + b2[2]) - b1[0];
        double push = Math.min(overlapL, overlapR) / 2.0;

        if (c1.x < c2.x) {
            c1.x -= push; c2.x += push;
        } else {
            c1.x += push; c2.x -= push;
        }
        // Slight velocity exchange
        double tmp = c1.velX * 0.2;
        c1.velX = c2.velX * 0.2;
        c2.velX = tmp;
    }

    private static boolean overlaps(double[] a, double[] b) {
        return a[0] < b[0]+b[2] && a[0]+a[2] > b[0]
                && a[1] < b[1]+b[3] && a[1]+a[3] > b[1];
    }

}