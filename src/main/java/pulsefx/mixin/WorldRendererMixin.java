package pulsefx.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pulsefx.FxConfig;
import pulsefx.SkyPresets;

/** Recolours the horizon / background fog colour to match the chosen sky. */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, require = 0)
    private Vector4f pulsefx$fog(Vector4f color) {
        if (FxConfig.sky != 0 && FxConfig.skyDimOk) {
            int c = SkyPresets.scaled(SkyPresets.HORIZON[FxConfig.sky], FxConfig.skyBrightness);
            return new Vector4f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, color.w);
        }
        return color;
    }
}
