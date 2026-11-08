package pulsefx;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Non-toggle settings + save/load of everything to config/pulsefx.properties. */
public final class FxConfig {
    public static final String[] TRAIL_STYLES = {"Rainbow Ribbon", "Aurora Helix", "Cosmic Dust", "Ice Crystal"};
    public static final String[] AURA_STYLES = {"Flame", "Frost", "Void", "Lightning", "Holy"};
    public static final String[] PERFORMANCE = {"Low", "Medium", "High"};
    private static final float[] PERF_MUL = {0.4f, 0.7f, 1.0f};

    public static int trailStyle = 0, auraStyle = 0, sky = 0, performance = 2;
    public static float skyBrightness = 1f, particleSize = 1f, rate = 1f, range = 1f;
    /** Watermark position (fraction of the screen) and size. */
    public static float wmX = 0.012f, wmY = 0.02f, wmScale = 1f;

    /** false in the Nether / End so their skies are left alone. Updated every tick. */
    public static boolean skyDimOk = true;

    private FxConfig() {}

    public static float perfMul() { return PERF_MUL[performance]; }

    public static void reset() {
        for (Fx f : Fx.values()) f.on = f.def;
        trailStyle = 0; auraStyle = 0; sky = 0; performance = 2;
        skyBrightness = 1f; particleSize = 1f; rate = 1f; range = 1f;
        wmX = 0.012f; wmY = 0.02f; wmScale = 1f;
    }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("pulsefx.properties"); }

    public static void save() {
        Properties p = new Properties();
        for (Fx f : Fx.values()) p.setProperty("fx." + f.name(), "" + f.on);
        p.setProperty("trailStyle", "" + trailStyle); p.setProperty("auraStyle", "" + auraStyle);
        p.setProperty("sky", "" + sky); p.setProperty("performance", "" + performance);
        p.setProperty("skyBrightness", "" + skyBrightness); p.setProperty("particleSize", "" + particleSize);
        p.setProperty("rate", "" + rate); p.setProperty("range", "" + range);
        p.setProperty("wmX", "" + wmX); p.setProperty("wmY", "" + wmY); p.setProperty("wmScale", "" + wmScale);
        try (OutputStream o = Files.newOutputStream(file())) { p.store(o, "PulseFX"); } catch (IOException ignored) {}
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (InputStream i = Files.newInputStream(f)) { p.load(i); } catch (IOException e) { return; }
        for (Fx x : Fx.values()) { String v = p.getProperty("fx." + x.name()); if (v != null) x.on = Boolean.parseBoolean(v); }
        trailStyle = clamp(i(p, "trailStyle", trailStyle), 0, TRAIL_STYLES.length - 1);
        auraStyle = clamp(i(p, "auraStyle", auraStyle), 0, AURA_STYLES.length - 1);
        sky = clamp(i(p, "sky", sky), 0, SkyPresets.NAMES.length - 1);
        performance = clamp(i(p, "performance", performance), 0, 2);
        skyBrightness = f(p, "skyBrightness", skyBrightness); particleSize = f(p, "particleSize", particleSize);
        rate = f(p, "rate", rate); range = f(p, "range", range);
        wmX = f(p, "wmX", wmX); wmY = f(p, "wmY", wmY); wmScale = Math.max(0.5f, Math.min(3f, f(p, "wmScale", wmScale)));
    }

    private static int i(Properties p, String k, int d) { try { return Integer.parseInt(p.getProperty(k)); } catch (Exception e) { return d; } }
    private static float f(Properties p, String k, float d) { try { return Float.parseFloat(p.getProperty(k)); } catch (Exception e) { return d; } }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
