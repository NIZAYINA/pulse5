package pulsefx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/** Screen-space effects: vignette, low-health pulse, speed lines, hit marker. They only touch the screen edges / crosshair. */
public final class HudFx {
    public static volatile long hitAtMs = 0;

    // watermark drag state
    private static boolean dragging = false, wasDown = false;
    private static double dragDX, dragDY;

    private HudFx() {}

    public static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        double now = System.nanoTime() / 1.0e9;

        if (Fx.WATERMARK.on) watermark(c, mc, p, w, h);

        if (Fx.VIGNETTE.on) vignette(c, w, h, 0x000000, 0.55f);

        if (Fx.LOW_HP.on && p.isAlive() && p.getHealth() <= 6f) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(now * 5.0);
            vignette(c, w, h, 0xB00000, 0.25f + 0.4f * pulse);
        }

        if (Fx.SPEED_LINES.on && p.isSprinting()) {
            Random r = new Random((long) (now * 20));
            double cx = w / 2.0, cy = h / 2.0, half = Math.hypot(w, h) / 2.0;
            for (int i = 0; i < 22; i++) {
                double a = r.nextDouble() * Math.PI * 2, r0 = 0.5 + r.nextDouble() * 0.3, len = 0.1 + r.nextDouble() * 0.15;
                for (int s = 0; s < 10; s++) {
                    double rr = (r0 + len * s / 9.0) * half;
                    int x = (int) (cx + Math.cos(a) * rr), y = (int) (cy + Math.sin(a) * rr);
                    if (x < 0 || y < 0 || x >= w || y >= h) continue;
                    int al = 30 + s * 9;
                    c.fill(x, y, x + 2, y + 2, (al << 24) | 0xFFFFFF);
                }
            }
        }

        if (Fx.HIT_MARKER.on) {
            long el = System.currentTimeMillis() - hitAtMs;
            if (el >= 0 && el < 260) {
                float k = el / 260f;
                int al = (int) (255 * (1 - k));
                int off = 4 + (int) (6 * k);
                int cx = w / 2, cy = h / 2, col = (al << 24) | 0xFFFFFF;
                for (int i = 0; i < 6; i++) {
                    int d = off + i;
                    c.fill(cx + d, cy + d, cx + d + 2, cy + d + 2, col);
                    c.fill(cx - d - 1, cy + d, cx - d + 1, cy + d + 2, col);
                    c.fill(cx + d, cy - d - 1, cx + d + 2, cy - d + 1, col);
                    c.fill(cx - d - 1, cy - d - 1, cx - d + 1, cy - d + 1, col);
                }
            }
        }
    }

    private static void vignette(DrawContext c, int w, int h, int rgb, float alpha) {
        int n = 16, tx = Math.max(3, w / 60), ty = Math.max(3, h / 60);
        for (int i = 0; i < n; i++) {
            float k = 1f - i / (float) n;
            int col = ((int) (alpha * k * k * 255) << 24) | rgb;
            c.fill(i * tx, 0, (i + 1) * tx, h, col);                 // left
            c.fill(w - (i + 1) * tx, 0, w - i * tx, h, col);         // right
            c.fill(0, i * ty, w, (i + 1) * ty, col);                 // top
            c.fill(0, h - (i + 1) * ty, w, h - i * ty, col);         // bottom
        }
    }

    // ---------------- watermark ----------------
    private static Text wmText(MinecraftClient mc, ClientPlayerEntity p) {
        String ping = "-- ms";
        if (mc.getNetworkHandler() != null && !mc.isInSingleplayer()) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (e != null) ping = e.getLatency() + " ms";
        }
        MutableText t = Text.literal("NIZAY").formatted(Formatting.BOLD, Formatting.WHITE);
        t.append(Text.literal("visuals").formatted(Formatting.GRAY));
        t.append(Text.literal("  |  ").formatted(Formatting.DARK_GRAY));
        t.append(Text.literal(mc.getCurrentFps() + " FPS").formatted(Formatting.WHITE));
        t.append(Text.literal("  |  ").formatted(Formatting.DARK_GRAY));
        t.append(Text.literal(ping).formatted(Formatting.WHITE));
        return t;
    }

    private static void watermark(DrawContext c, MinecraftClient mc, ClientPlayerEntity p, int w, int h) {
        Text t = wmText(mc, p);
        int bw = mc.textRenderer.getWidth(t) + 18, bh = 17;
        float s = FxConfig.wmScale;
        int sw = Math.round(bw * s), sh = Math.round(bh * s);
        int x = Math.max(0, Math.min(w - sw, Math.round(FxConfig.wmX * w)));
        int y = Math.max(0, Math.min(h - sh, Math.round(FxConfig.wmY * h)));

        boolean edit = mc.currentScreen instanceof ChatScreen;
        if (edit) {
            double mx = mc.mouse.getX() * w / mc.getWindow().getWidth();
            double my = mc.mouse.getY() * h / mc.getWindow().getHeight();
            boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

            int by = y >= 14 ? y - 13 : y + sh + 2;
            int minusX = x, plusX = x + 13;
            boolean overMinus = mx >= minusX && mx < minusX + 11 && my >= by && my < by + 11;
            boolean overPlus = mx >= plusX && mx < plusX + 11 && my >= by && my < by + 11;
            boolean overBox = mx >= x && mx < x + sw && my >= y && my < y + sh;

            if (down && !wasDown) {
                if (overMinus) { FxConfig.wmScale = Math.max(0.5f, Math.round((FxConfig.wmScale - 0.1f) * 10) / 10f); FxConfig.save(); }
                else if (overPlus) { FxConfig.wmScale = Math.min(3f, Math.round((FxConfig.wmScale + 0.1f) * 10) / 10f); FxConfig.save(); }
                else if (overBox) { dragging = true; dragDX = mx - x; dragDY = my - y; }
            }
            if (dragging && down) {
                double nx = Math.max(0, Math.min(w - sw, mx - dragDX)), ny = Math.max(0, Math.min(h - sh, my - dragDY));
                FxConfig.wmX = (float) (nx / w); FxConfig.wmY = (float) (ny / h);
                x = (int) nx; y = (int) ny;
                by = y >= 14 ? y - 13 : y + sh + 2;
                minusX = x; plusX = x + 13;
            }
            if (dragging && !down) { dragging = false; FxConfig.save(); }
            wasDown = down;

            // outline + resize buttons (only while chat is open)
            int ol = 0xAAFFFFFF;
            c.fill(x - 1, y - 1, x + sw + 1, y, ol);
            c.fill(x - 1, y + sh, x + sw + 1, y + sh + 1, ol);
            c.fill(x - 1, y, x, y + sh, ol);
            c.fill(x + sw, y, x + sw + 1, y + sh, ol);
            c.fill(minusX, by, minusX + 11, by + 11, 0xFF2E2E34);
            c.fill(plusX, by, plusX + 11, by + 11, 0xFF2E2E34);
            c.drawText(mc.textRenderer, Text.literal("-"), minusX + 4, by + 2, 0xFFFFFFFF, false);
            c.drawText(mc.textRenderer, Text.literal("+"), plusX + 3, by + 2, 0xFFFFFFFF, false);
        } else {
            dragging = false;
            wasDown = false;
        }

        var m = c.getMatrices();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(s, s);
        c.fillGradient(0, 0, bw, bh, 0xE02A2A30, 0xE0101012);
        c.fill(0, 0, bw, 1, 0xFF3A3A41);
        c.fill(0, bh - 1, bw, bh, 0xFF3A3A41);
        c.fill(0, 0, 2, bh, 0xFFDADCE2);
        c.drawText(mc.textRenderer, t, 8, 4, 0xFFFFFFFF, false);
        m.popMatrix();
    }
}
