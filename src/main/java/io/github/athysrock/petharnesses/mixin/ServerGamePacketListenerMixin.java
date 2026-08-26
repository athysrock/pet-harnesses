package io.github.athysrock.petharnesses.mixin;

import io.github.athysrock.petharnesses.network.CreativeSlotProtection;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rejects vanilla creative writes that could replace, remove, drop, or copy occupied authority. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerMixin {
    @Shadow public ServerPlayer player;

    @Inject(
            method = "handleSetCreativeModeSlot",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;hasInfiniteMaterials()Z",
                    shift = At.Shift.BEFORE),
            cancellable = true)
    private void petHarnesses$protectOccupiedHarness(
            ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (CreativeSlotProtection.reject(player, packet.slotNum(), packet.itemStack())) {
            player.inventoryMenu.broadcastFullState();
            ci.cancel();
        }
    }
}
