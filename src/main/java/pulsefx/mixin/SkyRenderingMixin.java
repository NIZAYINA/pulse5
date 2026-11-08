package pulsefx.mixin;

import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pulsefx.FxConfig;
import pulsefx.SkyPresets;

/**
 * Changes the sky colour inside the game's own sky renderer, so blocks, mobs and items are never tinted.
 * Works on any server (purely client side).
 */
@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    @ModifyVariable(method = "updateRenderState", at = @At("TAIL"), argsOnly = true, require = 0)
    private SkyRenderState pulsefx$sky(SkyRenderState state) {
        if (FxConfig.sky != 0 && FxConfig.skyDimOk) {
            state.skyColor = SkyPresets.scaled(SkyPresets.TOP[FxConfig.sky], FxConfig.skyBrightness);
            if (SkyPresets.STARS[FxConfig.sky]) state.starBrightness = Math.max(state.starBrightness, 0.9f);
        }
        return state;
    }
}
