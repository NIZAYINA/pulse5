package pulsefx;

/** 7 custom skies + vanilla. Colours are 0xRRGGBB. */
public final class SkyPresets {
    public static final String[] NAMES = {
        "Vanilla", "Pink Sunset", "Neon Night", "Aurora", "Lavender Dawn", "Deep Ocean", "Blood Moon", "Golden Hour"
    };
    public static final int[] TOP = {
        0x78A7FF, 0xD9609A, 0x1A0B4D, 0x0C5C57, 0x9B8CFF, 0x0A4F87, 0x5A0508, 0xF2A33A
    };
    public static final int[] HORIZON = {
        0xBFD8FF, 0xFFB07A, 0x8A3FE0, 0x4DFFC3, 0xFFD6E8, 0x5CC8E6, 0xE0302A, 0xFFE8A3
    };
    public static final boolean[] STARS = {false, false, true, true, false, true, true, false};

    /** Multiplies the brightness of an RGB colour and returns it as opaque ARGB. */
    public static int scaled(int rgb, float k) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * k));
        int b = Math.min(255, Math.round((rgb & 255) * k));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private SkyPresets() {}
}
