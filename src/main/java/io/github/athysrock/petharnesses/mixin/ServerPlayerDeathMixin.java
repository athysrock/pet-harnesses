package io.github.athysrock.petharnesses.mixin;

import io.github.athysrock.petharnesses.lifecycle.HarnessEquipmentLifecycle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after totem resolution but before messages, loot, inventory clearing, or respawn. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerDeathMixin {
    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), cancellable = true)
    private void petHarnesses$restoreBeforeDrops(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) return;
        if (!HarnessEquipmentLifecycle.restoreBeforeDroppingDeath(player)) {
            player.setHealth(1.0F);
            ci.cancel();
        }
    }
}
