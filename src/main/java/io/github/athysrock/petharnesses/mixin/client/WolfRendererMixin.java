package io.github.athysrock.petharnesses.mixin.client;

import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModAttachments;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Projects the synchronized harness attachment into vanilla's leash render state only. */
@Mixin(WolfRenderer.class)
abstract class WolfRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/wolf/Wolf;"
            + "Lnet/minecraft/client/renderer/entity/state/WolfRenderState;F)V", at = @At("RETURN"))
    private void petHarnesses$extractCustomTether(
            Wolf wolf, WolfRenderState state, float partialTicks, CallbackInfo ci) {
        HarnessLink link = wolf.getAttached(ModAttachments.PET_LINK);
        if (link == null || !link.petId().equals(wolf.getUUID())) return;

        Player owner = wolf.level().getPlayerByUUID(link.ownerId());
        if (owner == null) {
            state.leashStates = null;
            return;
        }

        Vec3 offset = wolf.getLeashOffset(partialTicks)
                .yRot(-wolf.getPreciseBodyRotation(partialTicks) * (float) (Math.PI / 180.0));
        Vec3 start = wolf.getPosition(partialTicks).add(offset);
        Vec3 end = owner.getRopeHoldPosition(partialTicks);
        BlockPos startEye = BlockPos.containing(wolf.getEyePosition(partialTicks));
        BlockPos endEye = BlockPos.containing(owner.getEyePosition(partialTicks));
        EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
        leash.offset = offset;
        leash.start = start;
        leash.end = end;
        leash.startBlockLight = wolf.level().getBrightness(LightLayer.BLOCK, startEye);
        leash.endBlockLight = wolf.level().getBrightness(LightLayer.BLOCK, endEye);
        leash.startSkyLight = wolf.level().getBrightness(LightLayer.SKY, startEye);
        leash.endSkyLight = wolf.level().getBrightness(LightLayer.SKY, endEye);
        leash.slack = true;
        state.leashStates = List.of(leash);
    }
}
