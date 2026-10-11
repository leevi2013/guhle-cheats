package de.ragzan.utility.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    @Invoker("register")
    static RenderPipeline ragzan$register(RenderPipeline pipeline) {
        throw new AssertionError("Mixin invoker should be replaced at runtime");
    }

    @Accessor("RENDERTYPE_LINES_SNIPPET")
    static RenderPipeline.Snippet ragzan$linesSnippet() {
        throw new AssertionError("Mixin accessor should be replaced at runtime");
    }
}
