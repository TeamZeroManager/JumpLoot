package com.teamzeromanager.jumploot.mixin;

import com.teamzeromanager.jumploot.JumpCounterHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void jumploot$trackJump(CallbackInfo callback) {
        if ((Object) this instanceof ServerPlayer player) {
            JumpCounterHandler.onPlayerJump(player);
        }
    }
}
