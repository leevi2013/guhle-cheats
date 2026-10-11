package de.ragzan.utility.mixin;

import de.ragzan.utility.ClientLogic;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FreecamMovementMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void ragzan$freezePlayerWhileFreecam(Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity && ClientLogic.isFreecamActive()) {
            ((LivingEntity) (Object) this).setVelocity(Vec3d.ZERO);
            ci.cancel();
        }
    }
}
