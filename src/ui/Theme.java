package ui;

import java.awt.*;

/**
 * Dark botanical illustration aesthetic.
 * Black background, glowing lines shift from amber (deep branches)
 * through green to bright yellow-white (fine tips).
 * Feels like looking at a bioluminescent plant in a dark room.
 */
public final class Theme {
    private Theme() {}

    public static final Color BG_VOID   = new Color(0x04070C);
    public static final Color BG_PANEL  = new Color(0x070C14);
    public static final Color BG_CARD   = new Color(0x0A1020);
    public static final Color BG_BORDER = new Color(0x101C2C);

    // Branch depth colours: deep (amber) → mid (green) → tip (bright)
    public static final Color BRANCH_DEEP = new Color(0xC06010);   // deep amber
    public static final Color BRANCH_MID  = new Color(0x40A040);   // green
    public static final Color BRANCH_TIP  = new Color(0xD0FF80);   // yellow-green

    // Grid / guides
    public static final Color GUIDE      = new Color(0x0C1828);
    public static final Color AXIS       = new Color(0x102030);

    // Text
    public static final Color TEXT_PRIMARY   = new Color(0xC8DDE8);
    public static final Color TEXT_SECONDARY = new Color(0x2A4A60);
    public static final Color TEXT_ACCENT    = new Color(0x60FF80);
    public static final Color TEXT_LABEL     = new Color(0x508070);

    // UI controls
    public static final Color BTN_ACCENT = new Color(0x40A060);
    public static final Color BTN_ALT    = new Color(0x4080C0);

    public static final Font FONT_TITLE  = new Font("Monospaced", Font.BOLD,  13);
    public static final Font FONT_LABEL  = new Font("Monospaced", Font.PLAIN, 10);
    public static final Font FONT_SMALL  = new Font("Monospaced", Font.PLAIN,  9);
    public static final Font FONT_HEADER = new Font("Monospaced", Font.BOLD,  10);
    public static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 11);

    /**
     * Colour a branch by its stack depth.
     * depth 0 = trunk/deep = amber
     * depth 5 = mid = green
     * depth 10+ = tip = bright yellow-green
     */
    public static Color branchColor(int depth, int alpha) {
        double t = Math.min(1.0, depth / 10.0);
        Color c;
        if (t < 0.5) {
            c = lerp(BRANCH_DEEP, BRANCH_MID, t * 2.0);
        } else {
            c = lerp(BRANCH_MID, BRANCH_TIP, (t - 0.5) * 2.0);
        }
        return withAlpha(c, alpha);
    }

    public static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                         Math.max(0, Math.min(255, alpha)));
    }
}
