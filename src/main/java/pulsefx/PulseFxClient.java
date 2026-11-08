package pulsefx;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * All effects are client-side particles / HUD drawing, so they work on any server.
 * Nothing here changes entity visibility: invisible players and mobs stay invisible.
 */
public class PulseFxClient implements ClientModInitializer {
    private static KeyBinding menuKey;
    private final Random rnd = new Random();
    private Vec3d lastPos = Vec3d.ZERO;
    private boolean wasOnGround = true, wasAttack = false, wasSprint = false;
    private double airTop = 0;
    private int tick, slashAge = -1;
    private double slashYaw;

    // colours for Glow Style: {from, to}
    private static final int[][] AURA = {
        {0xFFB03B, 0xFF3B1A}, // Flame
        {0xFFFFFF, 0x7FD6FF}, // Frost
        {0x2A0A4A, 0xB84CFF}, // Void
        {0xFFFF9A, 0x6AA8FF}, // Lightning
        {0xFFF2B0, 0xFFD35A}  // Holy
    };

    @Override
    public void onInitializeClient() {
        FxConfig.load();
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("pulsefx", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.pulsefx.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, cat));
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudElementRegistry.addFirst(Identifier.of("pulsefx", "hudfx"), (ctx, tc) -> HudFx.render(ctx));
    }

    // ---------------- helpers ----------------
    private int cnt(double base) {
        double v = base * FxConfig.rate * FxConfig.perfMul();
        int n = (int) v;
        if (rnd.nextDouble() < v - n) n++;
        return n;
    }
    private double j(double x) { return (rnd.nextDouble() - 0.5) * 2 * x; }
    private static DustParticleEffect dust(int rgb, float s) { return new DustParticleEffect(rgb, s); }
    private static DustColorTransitionParticleEffect trans(int a, int b, float s) { return new DustColorTransitionParticleEffect(a, b, s); }

    private static boolean holdingWeapon(ClientPlayerEntity p) {
        String id = Registries.ITEM.getId(p.getMainHandStack().getItem()).getPath();
        return id.endsWith("sword") || id.endsWith("_axe") || id.equals("trident") || id.equals("mace") || id.endsWith("spear");
    }

    // ---------------- main tick ----------------
    private void onTick(MinecraftClient mc) {
        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new FxScreen());
        }
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null || mc.isPaused()) return;
        tick++;

        FxConfig.skyDimOk = w.getRegistryKey() != World.NETHER && w.getRegistryKey() != World.END;

        boolean visible = !p.isInvisible();
        Vec3d pos = p.getEntityPos();
        float size = FxConfig.particleSize;
        double range = FxConfig.range;
        double yawR = Math.toRadians(p.getYaw()), pitR = Math.toRadians(p.getPitch());
        Vec3d fwd = new Vec3d(-Math.sin(yawR), 0, Math.cos(yawR));
        Vec3d right = new Vec3d(-Math.cos(yawR), 0, -Math.sin(yawR));
        double eyeY = p.getEyeY();

        // ================= VISUALS (ambient) =================
        if (Fx.STARS.on) {
            for (int i = cnt(1.5); i > 0; i--) {
                double a = rnd.nextDouble() * Math.PI * 2, r = (3 + rnd.nextDouble() * 10) * range;
                w.addParticleClient(ParticleTypes.END_ROD, pos.x + Math.cos(a) * r, pos.y + 0.5 + rnd.nextDouble() * 6,
                    pos.z + Math.sin(a) * r, 0, 0.01 + rnd.nextDouble() * 0.01, 0);
            }
        }
        if (Fx.SAKURA.on) {
            for (int i = cnt(0.7); i > 0; i--)
                w.addParticleClient(ParticleTypes.CHERRY_LEAVES, pos.x + j(8 * range), pos.y + 4 + rnd.nextDouble() * 3, pos.z + j(8 * range), 0, -0.02, 0);
        }
        if (Fx.MAGIC.on) {
            for (int i = cnt(2); i > 0; i--)
                w.addParticleClient(ParticleTypes.ENCHANT, pos.x, pos.y + 1.3, pos.z, j(1.6), rnd.nextDouble() * 1.4 - 0.6, j(1.6));
        }
        if (Fx.ORBS.on && visible) {
            for (int k = 0; k < 3; k++) {
                double a = tick * 0.12 + k * Math.PI * 2 / 3;
                double ox = pos.x + Math.cos(a) * 1.1, oz = pos.z + Math.sin(a) * 1.1, oy = pos.y + 1.0 + Math.sin(tick * 0.08 + k) * 0.35;
                w.addParticleClient(trans(0x6CF0FF, 0xFF6CE8, 1.1f * size), ox, oy, oz, 0, 0, 0);
                if (rnd.nextDouble() < 0.3) w.addParticleClient(ParticleTypes.END_ROD, ox, oy, oz, 0, 0, 0);
            }
        }
        if (Fx.WINGS.on && visible) {
            double flap = 1 + 0.18 * Math.sin(tick * 0.25);
            for (int i = cnt(5); i > 0; i--) {
                double t = rnd.nextDouble();
                for (int s = -1; s <= 1; s += 2) {
                    double lat = s * (0.25 + t * 1.25 * flap), up = 1.45 + Math.sin(t * Math.PI) * 0.75 - t * 0.35, back = 0.28 + t * 0.15;
                    w.addParticleClient(trans(0xFFFFFF, 0xFFD36B, 0.8f * size),
                        pos.x + right.x * lat - fwd.x * back, pos.y + up, pos.z + right.z * lat - fwd.z * back, 0, 0, 0);
                }
            }
        }
        if (Fx.HALO.on && visible) {
            for (int k = 0; k < 4; k++) {
                double a = tick * 0.15 + k * Math.PI / 2;
                w.addParticleClient(dust(0xFFD84A, 0.8f * size), pos.x + Math.cos(a) * 0.38, pos.y + 2.35, pos.z + Math.sin(a) * 0.38, 0, 0, 0);
            }
        }
        if (Fx.FIREFLIES.on) {
            for (int i = cnt(1.0); i > 0; i--)
                w.addParticleClient(dust(0xD8FF5A, 0.7f * size), pos.x + j(7 * range), pos.y + rnd.nextDouble() * 3, pos.z + j(7 * range), 0, 0, 0);
        }
        if (Fx.SNOWFALL.on) {
            for (int i = cnt(2); i > 0; i--)
                w.addParticleClient(ParticleTypes.SNOWFLAKE, pos.x + j(10 * range), pos.y + 5 + rnd.nextDouble() * 3, pos.z + j(10 * range), 0, -0.05, 0);
        }
        if (Fx.EMBERS.on) {
            for (int i = cnt(1.2); i > 0; i--) {
                w.addParticleClient(ParticleTypes.SMALL_FLAME, pos.x + j(6 * range), pos.y + rnd.nextDouble() * 2, pos.z + j(6 * range), 0, 0.05, 0);
                if (rnd.nextDouble() < 0.4) w.addParticleClient(ParticleTypes.ASH, pos.x + j(6 * range), pos.y + 1 + rnd.nextDouble() * 3, pos.z + j(6 * range), 0, 0.02, 0);
            }
        }
        if (Fx.WISPS.on) {
            for (int i = cnt(0.8); i > 0; i--)
                w.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, pos.x + j(5 * range), pos.y + rnd.nextDouble() * 2, pos.z + j(5 * range), 0, 0.03, 0);
        }
        if (Fx.HEARTS.on && visible) {
            if (cnt(0.15) > 0) w.addParticleClient(ParticleTypes.HEART, pos.x + j(0.5), pos.y + 2.2, pos.z + j(0.5), 0, 0.03, 0);
        }
        if (Fx.SPARKLES.on && visible) {
            for (int i = cnt(1.0); i > 0; i--)
                w.addParticleClient(ParticleTypes.END_ROD, pos.x + j(0.4), pos.y + rnd.nextDouble() * 1.8, pos.z + j(0.4), 0, 0, 0);
        }

        // ================= TRAIL =================
        Vec3d d = pos.subtract(lastPos);
        double dist = d.length();
        if (Fx.TRAIL.on && visible && dist > 0.04 && dist < 6) {
            int steps = (int) Math.min(10, Math.max(1, dist / 0.16));
            for (int s = 0; s < steps; s++)
                spawnTrail(w, lastPos.add(d.multiply((s + 1.0) / steps)), tick + s * 0.35, 0.9f * size);
        }

        // ================= MOVEMENT =================
        boolean sprint = p.isSprinting();
        if (Fx.FLAMES.on && visible && sprint && p.isOnGround()) {
            for (int i = cnt(2); i > 0; i--)
                w.addParticleClient(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, pos.x + j(0.2), pos.y + 0.05, pos.z + j(0.2), j(0.02), 0.03, j(0.02));
        }
        if (Fx.RIPPLES.on && visible && p.isOnGround() && dist > 0.08 && tick % 5 == 0) {
            for (int i = 0; i < 12; i++) {
                double a = i / 12.0 * Math.PI * 2;
                w.addParticleClient(dust(0xFFFFFF, 0.5f * size), pos.x + Math.cos(a) * 0.35, pos.y + 0.05, pos.z + Math.sin(a) * 0.35, 0, 0, 0);
            }
        }
        if (Fx.DASH.on && visible && sprint && !wasSprint && p.isOnGround()) {
            for (int i = 0; i < 14; i++)
                w.addParticleClient(ParticleTypes.CLOUD, pos.x + j(0.3), pos.y + 0.1 + rnd.nextDouble() * 0.4, pos.z + j(0.3),
                    -fwd.x * 0.2 + j(0.05), 0.02, -fwd.z * 0.2 + j(0.05));
        }
        wasSprint = sprint;

        if (!p.isOnGround()) {
            if (wasOnGround) airTop = pos.y;
            airTop = Math.max(airTop, pos.y);
        }
        if (Fx.JUMP_RINGS.on && visible && wasOnGround && !p.isOnGround() && p.getVelocity().y > 0.1) {
            for (int i = 0; i < 28; i++) {
                double a = i / 28.0 * Math.PI * 2;
                w.addParticleClient(dust(MathHelper.hsvToRgb(i / 28f, 0.45f, 1f), 1.0f * size), pos.x + Math.cos(a) * 0.55, pos.y + 0.1, pos.z + Math.sin(a) * 0.55, 0, 0, 0);
                if (i % 3 == 0) w.addParticleClient(ParticleTypes.END_ROD, pos.x, pos.y + 0.1, pos.z, Math.cos(a) * 0.16, 0.02, Math.sin(a) * 0.16);
            }
        }
        if (Fx.SHOCKWAVE.on && visible && p.isOnGround() && !wasOnGround && airTop - pos.y > 2.5) {
            for (int i = 0; i < 36; i++) {
                double a = i / 36.0 * Math.PI * 2;
                w.addParticleClient(ParticleTypes.CLOUD, pos.x, pos.y + 0.1, pos.z, Math.cos(a) * 0.28, 0.02, Math.sin(a) * 0.28);
                if (i % 2 == 0) w.addParticleClient(dust(0xCFC3A8, 1.2f * size), pos.x + Math.cos(a) * 0.8, pos.y + 0.1, pos.z + Math.sin(a) * 0.8, 0, 0, 0);
            }
        }
        wasOnGround = p.isOnGround();
        lastPos = pos;

        // ================= COMBAT =================
        if (Fx.AURA.on && visible && holdingWeapon(p)) {
            int[] c = AURA[FxConfig.auraStyle];
            double bx = pos.x + right.x * 0.45 + fwd.x * 0.5, by = eyeY - 0.55, bz = pos.z + right.z * 0.45 + fwd.z * 0.5;
            for (int i = cnt(3); i > 0; i--) {
                double t = 0.1 + rnd.nextDouble() * 1.0;
                double ax = bx + fwd.x * 0.6 * t + j(0.05), ay = by + 0.8 * t, az = bz + fwd.z * 0.6 * t + j(0.05);
                w.addParticleClient(trans(c[0], c[1], 0.7f * size), ax, ay, az, 0, 0, 0);
                if (rnd.nextDouble() < 0.35) {
                    switch (FxConfig.auraStyle) {
                        case 0 -> w.addParticleClient(ParticleTypes.FLAME, ax, ay, az, 0, 0.02, 0);
                        case 1 -> w.addParticleClient(ParticleTypes.SNOWFLAKE, ax, ay, az, 0, -0.01, 0);
                        case 2 -> w.addParticleClient(ParticleTypes.PORTAL, ax, ay, az, j(0.3), j(0.3), j(0.3));
                        case 3 -> w.addParticleClient(ParticleTypes.ELECTRIC_SPARK, ax, ay, az, j(0.2), j(0.2), j(0.2));
                        default -> w.addParticleClient(ParticleTypes.END_ROD, ax, ay, az, 0, 0.01, 0);
                    }
                }
            }
        }

        boolean atk = mc.options.attackKey.isPressed();
        if (atk && !wasAttack && mc.currentScreen == null) {
            if (Fx.SLASH.on && visible) { slashAge = 0; slashYaw = yawR; }
            if (mc.crosshairTarget instanceof EntityHitResult ehr) {
                Entity e = ehr.getEntity();
                if (e instanceof LivingEntity && !e.isInvisible()) onHit(w, ehr.getPos(), size);
            }
        }
        if (Fx.MINING.on && atk && mc.currentScreen == null && mc.crosshairTarget instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            Vec3d hp = bh.getPos();
            Direction sd = bh.getSide();
            for (int i = cnt(2); i > 0; i--)
                w.addParticleClient(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                    hp.x + sd.getOffsetX() * 0.05 + j(0.25), hp.y + sd.getOffsetY() * 0.05 + j(0.25), hp.z + sd.getOffsetZ() * 0.05 + j(0.25),
                    sd.getOffsetX() * 0.12 + j(0.06), sd.getOffsetY() * 0.12 + rnd.nextDouble() * 0.1, sd.getOffsetZ() * 0.12 + j(0.06));
        }
        wasAttack = atk;

        if (slashAge >= 0) {
            drawSlash(w, pos.x, eyeY - 0.35, pos.z, size);
            slashAge++;
            if (slashAge > 5) slashAge = -1;
        }
    }

    // ---------------- trail styles ----------------
    private void spawnTrail(ClientWorld w, Vec3d q, double t, float sz) {
        switch (FxConfig.trailStyle) {
            case 0 -> { // Rainbow Ribbon
                for (int k = 0; k < 2; k++) {
                    int c = MathHelper.hsvToRgb((float) (((t * 6 + k * 40) % 360) / 360.0), 0.6f, 1f);
                    w.addParticleClient(dust(c, sz), q.x + j(0.1), q.y + 0.25 + k * 0.8, q.z + j(0.1), 0, 0, 0);
                }
            }
            case 1 -> { // Aurora Helix
                for (int k = 0; k < 2; k++) {
                    double a = t * 0.6 + k * Math.PI;
                    w.addParticleClient(trans(0x4DFFC3, 0x8A5CFF, sz), q.x + Math.cos(a) * 0.45, q.y + 0.9 + Math.sin(t * 0.3) * 0.5, q.z + Math.sin(a) * 0.45, 0, 0, 0);
                }
                if (rnd.nextDouble() < 0.15) w.addParticleClient(ParticleTypes.END_ROD, q.x + j(0.3), q.y + 0.9, q.z + j(0.3), 0, 0.01, 0);
            }
            case 2 -> { // Cosmic Dust
                w.addParticleClient(trans(0xFF4FD8, 0x3A2BFF, sz), q.x + j(0.22), q.y + 0.2 + rnd.nextDouble() * 1.4, q.z + j(0.22), 0, 0, 0);
                if (rnd.nextDouble() < 0.2) w.addParticleClient(ParticleTypes.END_ROD, q.x + j(0.3), q.y + 0.2 + rnd.nextDouble() * 1.6, q.z + j(0.3), 0, 0.005, 0);
            }
            default -> { // Ice Crystal
                w.addParticleClient(trans(0xFFFFFF, 0x7FD6FF, sz), q.x + j(0.2), q.y + 0.15 + rnd.nextDouble() * 1.2, q.z + j(0.2), 0, 0, 0);
                if (rnd.nextDouble() < 0.3) w.addParticleClient(ParticleTypes.SNOWFLAKE, q.x + j(0.25), q.y + 0.4 + rnd.nextDouble(), q.z + j(0.25), 0, -0.02, 0);
            }
        }
    }

    // ---------------- slash arc (replaces the plain swing look) ----------------
    private void drawSlash(ClientWorld w, double cx, double cy, double cz, float size) {
        int[] c = AURA[FxConfig.auraStyle];
        for (int k = 0; k < 6; k++) {
            double ang = -75 + slashAge * 28 + k * 5.6;
            double a = slashYaw + Math.toRadians(ang);
            double dx = -Math.sin(a), dz = Math.cos(a), dy = (ang / 75.0) * 0.35 - 0.1;
            for (double R = 1.6; R <= 2.3; R += 0.35) {
                w.addParticleClient(trans(c[0], c[1], 1.0f * size), cx + dx * R, cy + dy, cz + dz * R, 0, 0, 0);
            }
            if (k % 2 == 0) w.addParticleClient(ParticleTypes.END_ROD, cx + dx * 2.0, cy + dy, cz + dz * 2.0, dx * 0.05, 0, dz * 0.05);
        }
    }

    // ---------------- hit effects ----------------
    private void onHit(ClientWorld w, Vec3d at, float size) {
        HudFx.hitAtMs = System.currentTimeMillis();
        if (Fx.SPARKS.on) {
            for (int i = cnt(22); i > 0; i--) {
                double a = rnd.nextDouble() * Math.PI * 2, sp = 0.15 + rnd.nextDouble() * 0.3, up = rnd.nextDouble() * 0.35;
                double vx = Math.cos(a) * sp, vz = Math.sin(a) * sp;
                switch (i % 3) {
                    case 0 -> w.addParticleClient(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, vx, up, vz);
                    case 1 -> w.addParticleClient(ParticleTypes.FIREWORK, at.x, at.y, at.z, vx * 0.6, up, vz * 0.6);
                    default -> w.addParticleClient(trans(0xFFE08A, 0xFF5A3C, 1.2f * size), at.x + j(0.15), at.y + j(0.15), at.z + j(0.15), 0, 0, 0);
                }
            }
        }
        if (Fx.CRIT.on) {
            for (int i = cnt(18); i > 0; i--) {
                double a = rnd.nextDouble() * Math.PI * 2, sp = 0.2 + rnd.nextDouble() * 0.3;
                w.addParticleClient(i % 3 == 0 ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT, at.x, at.y, at.z, Math.cos(a) * sp, rnd.nextDouble() * 0.4, Math.sin(a) * sp);
            }
        }
        if (Fx.HIT_RINGS.on) {
            int[] c = AURA[FxConfig.auraStyle];
            for (int i = 0; i < 28; i++) {
                double a = i / 28.0 * Math.PI * 2;
                w.addParticleClient(ParticleTypes.END_ROD, at.x, at.y, at.z, Math.cos(a) * 0.22, 0, Math.sin(a) * 0.22);
                if (i % 2 == 0) w.addParticleClient(trans(c[0], c[1], 1.0f * size), at.x + Math.cos(a) * 0.5, at.y, at.z + Math.sin(a) * 0.5, 0, 0, 0);
            }
        }
    }
}
