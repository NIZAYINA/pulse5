package pulsefx;

/** Every on/off effect. tab: 0 Visuals, 1 Movement, 2 Combat, 3 Screen. */
public enum Fx {
    // ---- Visuals ----
    STARS(0, "Stars Around Player", "Glittering stars float up around you.", true),
    SAKURA(0, "Sakura Petals", "Cherry-blossom petals drift down around you.", false),
    TRAIL(0, "Color Trail", "A glowing ribbon follows you. Choose its style with Trail Style below.", true),
    JUMP_RINGS(0, "Jump Rings", "A ring of light bursts out of your feet when you jump.", true),
    MAGIC(0, "Magic Particles", "Enchanting-table runes swirl into you.", false),
    ORBS(0, "Orbiting Orbs", "Three glowing orbs circle around your body.", false),
    WINGS(0, "Angel Wings", "Flapping wings of light on your back (best in third person, F5).", false),
    HALO(0, "Halo", "A golden ring floats above your head (third person).", false),
    FIREFLIES(0, "Fireflies", "Soft yellow-green fireflies hover around you.", false),
    SNOWFALL(0, "Snowfall", "Snowflakes fall around you, whatever the weather.", false),
    EMBERS(0, "Ember Rain", "Glowing embers and ash rise around you.", false),
    WISPS(0, "Soul Wisps", "Blue soul flames drift up around you.", false),
    HEARTS(0, "Love Hearts", "Small hearts pop up above your head now and then.", false),
    SPARKLES(0, "Body Sparkles", "Tiny sparkles glitter all over your body.", false),
    // ---- Movement ----
    FLAMES(1, "Run Flames", "Little flames burn at your feet while you sprint.", false),
    RIPPLES(1, "Footstep Ripples", "Each few steps leave a small ring on the ground.", false),
    SHOCKWAVE(1, "Landing Shockwave", "A dust shockwave spreads out when you land from a high fall.", false),
    DASH(1, "Sprint Puff", "A puff of clouds shoots out behind you when you start sprinting.", false),
    // ---- Combat ----
    SPARKS(2, "Impact Sparks", "Sparks fly from the target when you hit it. Invisible targets are skipped.", true),
    CRIT(2, "Crit Stars", "Extra critical-hit stars burst out of the target.", false),
    SLASH(2, "Swing Arc", "Every swing draws a glowing crescent in the air (colour follows Glow Style).", false),
    HIT_RINGS(2, "Impact Rings", "A ring of light expands from the target when you hit it.", false),
    AURA(2, "Weapon Glow", "Your sword, axe, trident or mace glows with the Glow Style you pick below.", false),
    MINING(2, "Mining Sparks", "Sparks fly off the block while you hold the attack button on it.", false),
    HIT_MARKER(2, "Impact Cross", "A small X flashes on your crosshair when you hit something.", false),
    // ---- Screen ----
    WATERMARK(3, "Watermark", "Shows NIZAYvisuals with your FPS and ping. Open chat (T) to drag it anywhere and resize it with the + and - buttons.", false),
    VIGNETTE(3, "Soft Vignette", "Darkens the screen edges for a cinematic look.", false),
    LOW_HP(3, "Low Health Pulse", "The screen edges pulse red when your health is low.", false),
    SPEED_LINES(3, "Motion Lines", "Thin streaks fly past the screen edges while you sprint.", false);

    public final int tab;
    public final String label, desc;
    public final boolean def;
    public boolean on;

    Fx(int tab, String label, String desc, boolean def) {
        this.tab = tab; this.label = label; this.desc = desc; this.def = def; this.on = def;
    }
}
