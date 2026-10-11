package de.ragzan.utility.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderLayer.class)
public interface RenderLayerAccessor {
    @Invoker("of")
    static RenderLayer ragzan$create(String name, RenderSetup setup) {
        throw new AssertionError("Mixin invoker should be replaced at runtime");
    }
}
