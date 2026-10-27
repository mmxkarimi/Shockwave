package ir.mmxkarimi.shockwave.ui.widget;

/** Pure polar shape math for the play button morph: a circle (paused) to a 10-lobe cookie (playing). */
public final class ShapeMath {
    public static final int COOKIE_LOBES = 10;
    static final float COOKIE_DEPTH = 0.075f;

    private ShapeMath() {}

    /** Unit-radius distance of the cookie edge at angle {@code t}; lobe tips at 1, valleys inside. */
    public static float cookie(double t) {
        return (float) ((1.0 + COOKIE_DEPTH * Math.cos(COOKIE_LOBES * t)) / (1.0 + COOKIE_DEPTH));
    }

    /** Morph between circle (m=0) and cookie (m=1); m may overshoot slightly for spring feel. Never wider than
     * the unit radius on either end, so the shape always fits the square view. */
    public static float radius(double t, float m) {
        return 1f + (cookie(t) - 1f) * m;
    }
}
