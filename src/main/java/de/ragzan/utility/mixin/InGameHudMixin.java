package de.ragzan.utility.mixin;

import de.ragzan.utility.ClientLogic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void ragzan$stashAlert(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        String message = ClientLogic.hudMessage();
        if (message == null || message.isBlank()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int textWidth = client.textRenderer.getWidth(message);
        int boxWidth = Math.min(client.getWindow().getScaledWidth() - 12, Math.max(235, textWidth + 25));
        context.fill(6, 6, 6 + boxWidth, 39, 0xE0121725);
        context.fill(6, 6, 9, 39, 0xFFFFD45A);
        context.drawTextWithShadow(client.textRenderer, Text.literal("RAGZAN UTILITY · ERKENNUNG"), 15, 10, 0xFFFFD45A);
        context.drawTextWithShadow(client.textRenderer, Text.literal(message), 15, 23, 0xFFFFFFFF);
    }
}
