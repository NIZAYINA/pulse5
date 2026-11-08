package pulsefx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Dark graphite settings menu (opens with P). Hover any option to read what it does. */
public class FxScreen extends Screen {
    private static final int TEXT = 0xFFD2D4DA, TEXT_DIM = 0xFF8C8F98, ACCENT = 0xFFDADCE2;
    private static final String[] TABS = {"Ambient", "Motion", "Impact", "Overlay", "Sky", "Settings", "About"};
    private static int tab = 0;

    private int px, py, pw, ph;
    private final List<Object[]> labels = new ArrayList<>(); // {x, y, text}

    public FxScreen() { super(Text.literal("PulseFX")); }

    private static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    // ---------- widgets: vanilla ButtonWidget with our own look painted on top ----------
    private interface Painter { void paint(DrawContext c, ClickableWidget b); }
    private record Entry(ClickableWidget w, Painter painter, String desc) {}
    private final List<Entry> entries = new ArrayList<>();

    private void button(int x, int y, int w, int h, Runnable press, Painter painter, String desc) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(""), btn -> press.run()).dimensions(x, y, w, h).build();
        addDrawableChild(b);
        entries.add(new Entry(b, painter, desc));
    }

    private void tabButton(int x, int y, int w, int h, String label, BooleanSupplier selected, Runnable press) {
        button(x, y, w, h, press, (c, b) -> {
            boolean hov = b.isHovered(), sel = selected.getAsBoolean();
            int bx = b.getX(), by = b.getY(), bw = b.getWidth(), bh = b.getHeight();
            c.fill(bx, by, bx + bw, by + bh, sel ? 0xFF44444B : (hov ? 0xFF2E2E34 : 0xFF202024));
            if (sel) c.fill(bx, by, bx + 2, by + bh, ACCENT);
            text(c, label, bx + 14, by + (bh - 8) / 2, sel ? 0xFFFFFFFF : (hov ? TEXT : TEXT_DIM));
        }, null);
    }

    private void actionButton(int x, int y, int w, int h, String label, boolean primary, Runnable press, String desc) {
        button(x, y, w, h, press, (c, b) -> {
            boolean hov = b.isHovered();
            int bx = b.getX(), by = b.getY(), bw = b.getWidth(), bh = b.getHeight();
            if (primary) c.fillGradient(bx, by, bx + bw, by + bh, hov ? 0xFFEDEEF1 : 0xFFD6D8DD, hov ? 0xFFB9BCC4 : 0xFF9EA2AB);
            else c.fillGradient(bx, by, bx + bw, by + bh, hov ? 0xFF3A3A41 : 0xFF2E2E34, hov ? 0xFF26262B : 0xFF1C1C20);
            text(c, label, bx + (bw - font().getWidth(label)) / 2, by + (bh - 8) / 2, primary ? 0xFF15151A : TEXT);
        }, desc);
    }

    private void toggle(int x, int y, int w, int h, Fx fx) {
        button(x, y, w, h, () -> fx.on = !fx.on, (c, b) -> {
            boolean on = fx.on, hov = b.isHovered();
            int bx = b.getX(), by = b.getY(), bw = b.getWidth(), bh = b.getHeight();
            c.fill(bx, by, bx + bw, by + bh, hov ? 0xFF26262B : 0xFF1B1B1F);
            text(c, fx.label, bx + 6, by + (bh - 8) / 2, on ? TEXT : TEXT_DIM);
            int sx = bx + bw - 30, sy = by + (bh - 12) / 2;
            c.fill(sx, sy, sx + 24, sy + 12, on ? 0xFFC9CCD3 : 0xFF3A3A41);
            int kx = on ? sx + 13 : sx + 1;
            c.fill(kx, sy + 1, kx + 10, sy + 11, on ? 0xFF16161A : 0xFF8C8F98);
        }, fx.desc);
    }

    private void cycle(int x, int y, int w, int h, String label, Supplier<String> value, Runnable next, String desc) {
        button(x, y, w, h, next, (c, b) -> {
            boolean hov = b.isHovered();
            int bx = b.getX(), by = b.getY(), bw = b.getWidth(), bh = b.getHeight();
            c.fill(bx, by, bx + bw, by + bh, hov ? 0xFF26262B : 0xFF1B1B1F);
            text(c, label, bx + 6, by + (bh - 8) / 2, TEXT);
            int boxW = 124, boxX = bx + bw - boxW - 4;
            c.fill(boxX - 1, by + 1, boxX + boxW + 1, by + bh - 1, 0xFF3A3A41);
            c.fillGradient(boxX, by + 2, boxX + boxW, by + bh - 2, 0xFF2C2C32, 0xFF18181B);
            String s = value.get();
            text(c, s, boxX + (boxW - font().getWidth(s)) / 2, by + (bh - 8) / 2, 0xFFF2F3F5);
            text(c, ">", boxX + boxW - 10, by + (bh - 8) / 2, TEXT_DIM);
        }, desc);
    }

    private void slider(int cx, int y, int cw, String label, float min, float max, Supplier<Float> get, Consumer<Float> set, String desc) {
        labels.add(new Object[]{cx + 8, y + 7, label});
        Slide s = new Slide(cx + cw - 150, y, 104, 22, min, max, get, set);
        addDrawableChild(s);
        entries.add(new Entry(s, null, desc));
    }

    // ================= layout =================
    @Override
    protected void init() {
        entries.clear();
        labels.clear();
        pw = Math.min(470, width - 12);
        ph = Math.min(330, height - 10);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        int sp = Math.min(26, (ph - 100) / TABS.length);
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            tabButton(px + 10, py + 54 + i * sp, 104, sp - 2, TABS[i], () -> idx == tab, () -> { tab = idx; clearAndInit(); });
        }
        actionButton(px + pw / 2 - 102, py + ph - 32, 96, 22, "Defaults", false, () -> { FxConfig.reset(); clearAndInit(); }, "Reset every setting to its default.");
        actionButton(px + pw / 2 + 6, py + ph - 32, 96, 22, "Save", true, () -> { FxConfig.save(); close(); }, "Save settings and close the menu.");

        int cx = px + 128, cw = pw - 128 - 14, y0 = py + 64;
        int rowH = ph < 300 ? 19 : 20, step = rowH + 2;

        if (tab <= 3) {
            int colW = (cw - 8) / 2, i = 0;
            for (Fx f : Fx.values()) {
                if (f.tab != tab) continue;
                toggle(cx + (i % 2) * (colW + 8), y0 + (i / 2) * step, colW, rowH, f);
                i++;
            }
            int extraY = y0 + ((i + 1) / 2) * step + 6;
            if (tab == 0) {
                cycle(cx, extraY, cw, rowH + 2, "Trail Style", () -> FxConfig.TRAIL_STYLES[FxConfig.trailStyle],
                    () -> FxConfig.trailStyle = (FxConfig.trailStyle + 1) % FxConfig.TRAIL_STYLES.length,
                    "Look of the Color Trail. Rainbow Ribbon, Aurora Helix, Cosmic Dust or Ice Crystal.");
            } else if (tab == 2) {
                cycle(cx, extraY, cw, rowH + 2, "Glow Style", () -> FxConfig.GLOW_STYLES[FxConfig.auraStyle],
                    () -> FxConfig.auraStyle = (FxConfig.auraStyle + 1) % FxConfig.GLOW_STYLES.length,
                    "Weapon skin look used by Weapon Glow, Swing Arc and Impact Rings: Flame, Frost, Void, Lightning or Holy.");
            }
        } else if (tab == 4) {
            cycle(cx, y0, cw, 22, "Sky Preset", () -> SkyPresets.NAMES[FxConfig.sky],
                () -> FxConfig.sky = (FxConfig.sky + 1) % SkyPresets.NAMES.length,
                "Replaces the sky colour of the Overworld. Works on servers too. Nether and End are left alone.");
            slider(cx, y0 + 28, cw, "Sky Brightness", 0.3f, 1.5f, () -> FxConfig.skyBrightness, v -> FxConfig.skyBrightness = v,
                "Makes the custom sky darker or brighter.");
        } else if (tab == 5) {
            slider(cx, y0, cw, "Particle Size", 0.5f, 2.0f, () -> FxConfig.particleSize, v -> FxConfig.particleSize = v,
                "Size of the coloured trail, ring and aura particles.");
            slider(cx, y0 + 28, cw, "Effect Rate", 0.25f, 2.0f, () -> FxConfig.rate, v -> FxConfig.rate = v,
                "How many particles are spawned. Lower it if the game lags.");
            slider(cx, y0 + 56, cw, "Effect Range", 0.5f, 2.0f, () -> FxConfig.range, v -> FxConfig.range = v,
                "How far from you stars, petals, snow and embers appear.");
            cycle(cx, y0 + 90, cw, 22, "Performance", () -> FxConfig.PERFORMANCE[FxConfig.performance],
                () -> FxConfig.performance = (FxConfig.performance + 1) % FxConfig.PERFORMANCE.length,
                "Overall particle budget: Low uses 40 percent, Medium 70 percent, High 100 percent.");
        }
    }

    // ================= drawing =================
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fillGradient(0, 0, width, height, 0xC0121214, 0xE0050506);
        c.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF34343A);
        c.fillGradient(px, py, px + pw, py + ph, 0xFF2B2B30, 0xFF0E0E10);

        TextRenderer f = font();
        c.drawText(f, Text.literal("PulseFX").formatted(Formatting.BOLD), px + 14, py + 10, 0xFFF2F3F5, true);
        c.drawText(f, Text.literal("Visual effects settings"), px + 14, py + 24, TEXT_DIM, false);
        String ver = "v1.1.0";
        c.drawText(f, Text.literal(ver), px + pw - 14 - f.getWidth(ver), py + 12, TEXT_DIM, false);
        c.fill(px + 10, py + 42, px + pw - 10, py + 43, 0xFF3A3A41);
        c.fill(px + 120, py + 52, px + 121, py + ph - 42, 0xFF2F2F35);
        c.fill(px + 10, py + ph - 40, px + pw - 10, py + ph - 39, 0xFF3A3A41);

        int cx = px + 128, cw = pw - 128 - 14;
        c.drawText(f, Text.literal(TABS[tab].toUpperCase()), cx, py + 50, TEXT_DIM, false);

        String info = null;
        for (Entry e : entries) {
            e.w().render(c, mx, my, delta);
            if (e.painter() != null) e.painter().paint(c, e.w());
        }
        for (Entry e : entries) if (e.w().isHovered() && e.desc() != null) info = e.desc();
        for (Object[] l : labels) c.drawText(f, Text.literal((String) l[2]), (Integer) l[0], (Integer) l[1], TEXT, false);

        if (tab == 4) { // sky preview
            int sy = py + 64 + 60, sh = Math.max(40, Math.min(70, ph - 64 - 60 - 120));
            int top = SkyPresets.scaled(SkyPresets.TOP[FxConfig.sky], FxConfig.skyBrightness);
            int hor = SkyPresets.scaled(SkyPresets.HORIZON[FxConfig.sky], FxConfig.skyBrightness);
            c.fill(cx - 1, sy - 1, cx + cw + 1, sy + sh + 1, 0xFF3A3A41);
            c.fillGradient(cx, sy, cx + cw, sy + sh, top, hor);
            if (SkyPresets.STARS[FxConfig.sky]) {
                for (int i = 0; i < 30; i++) {
                    int sx = cx + 6 + (i * 53) % (cw - 12), sty = sy + 4 + (i * 29) % Math.max(8, sh - 22);
                    c.fill(sx, sty, sx + 1, sty + 1, 0xCCFFFFFF);
                }
            }
            c.drawText(f, Text.literal("Preview"), cx + 6, sy + sh - 12, 0xFFFFFFFF, true);
        }
        if (tab == 6) {
            String[] lines = {
                "PulseFX - client-side visual effects.",
                "Version 1.1.0  |  Minecraft 1.21.11  |  Fabric",
                "",
                "Press P in game to open this menu.",
                "Settings are saved to config/pulsefx.properties.",
                "Everything is client-side, so it works on servers.",
                "Watermark: open chat (T), then drag it and use + / -.",
                "Invisible players are never revealed by this mod."
            };
            for (int i = 0; i < lines.length; i++) c.drawText(f, Text.literal(lines[i]), cx, py + 68 + i * 14, i == 0 ? TEXT : TEXT_DIM, false);
        }

        if (tab <= 5) { // info box: what the hovered option does
            int iy = py + ph - 78, ih = 34;
            c.fill(cx - 1, iy - 1, cx + cw + 1, iy + ih + 1, 0xFF34343A);
            c.fill(cx, iy, cx + cw, iy + ih, 0xFF141417);
            String txt = info == null ? "Hover an option to see what it does." : info;
            List<String> ls = wrap(txt, cw - 14);
            for (int i = 0; i < Math.min(2, ls.size()); i++) text(c, ls.get(i), cx + 7, iy + 6 + i * 12, info == null ? TEXT_DIM : TEXT);
        }
    }

    private static List<String> wrap(String s, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String t = line.length() == 0 ? word : line + " " + word;
            if (font().getWidth(t) > maxW && line.length() > 0) { out.add(line.toString()); line = new StringBuilder(word); }
            else line = new StringBuilder(t);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    @Override
    public void removed() { FxConfig.save(); }

    @Override
    public boolean shouldPause() { return false; }

    private static void text(DrawContext c, String s, int x, int y, int col) {
        c.drawText(font(), Text.literal(s), x, y, col, false);
    }

    // ================= slider =================
    static class Slide extends SliderWidget {
        private final float min, max; private final Consumer<Float> set;
        private boolean ready = false;
        Slide(int x, int y, int w, int h, float min, float max, Supplier<Float> get, Consumer<Float> set) {
            super(x, y, w, h, Text.empty(), (get.get() - min) / (max - min));
            this.min = min; this.max = max; this.set = set;
            this.ready = true;
            updateMessage();
        }
        @Override protected void updateMessage() {
            if (!ready) return;
            setMessage(Text.literal(Math.round((min + value * (max - min)) * 100) + "%"));
        }
        @Override protected void applyValue() { set.accept((float) (min + value * (max - min))); }
        @Override
        public void renderWidget(DrawContext c, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int ty = y + h / 2 - 2;
            c.fill(x, ty, x + w, ty + 4, 0xFF34343A);
            int fx = x + (int) (value * w);
            c.fill(x, ty, fx, ty + 4, 0xFFC9CCD3);
            int kx = Math.max(x, Math.min(x + w - 6, fx - 3));
            c.fill(kx, y + 3, kx + 6, y + h - 3, isHovered() ? 0xFFFFFFFF : 0xFFE4E6EA);
            text(c, getMessage().getString(), x + w + 8, y + (h - 8) / 2, TEXT_DIM);
        }
    }
}
