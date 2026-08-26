package io.github.athysrock.petharnesses.mixin.client;

import io.github.athysrock.petharnesses.client.CarriedWolfMotionSnapshot;
import io.github.athysrock.petharnesses.client.ClientRenderDataKeys;
import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.component.PetRenderData;
import io.github.athysrock.petharnesses.item.ModItems;
import io.github.athysrock.petharnesses.physics.MotionInput;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the equipped-harness flag plus compact occupied-pet data and local motion into render state. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
    private void petHarnesses$extractCarriedWolf(
            Avatar player, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        state.setData(ClientRenderDataKeys.HARNESS_EQUIPPED, false);
        state.setData(ClientRenderDataKeys.PET, null);
        state.setData(ClientRenderDataKeys.MOTION, null);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        boolean harnessEquipped = legs.is(ModItems.PET_HARNESS);
        state.setData(ClientRenderDataKeys.HARNESS_EQUIPPED, harnessEquipped);
        PetRenderData renderData = legs.get(ModComponents.PET_RENDER_DATA);
        HarnessLink link = legs.get(ModComponents.HARNESS_LINK);
        if (!harnessEquipped
                || renderData == null
                || link == null
                || !renderData.petId().equals(link.petId())) return;

        Vec3 current = player.getDeltaMovement();
        Vec3 previous = player instanceof ClientAvatarEntity client
                ? client.avatarState().deltaMovementOnPreviousTick()
                : current;
        MotionInput input = new MotionInput(
                current.horizontalDistance(),
                current.y,
                current.y - previous.y,
                Math.toRadians(Mth.wrapDegrees(player.yBodyRot - player.yBodyRotO)),
                Math.toRadians(player.getXRot()),
                player.onGround(),
                player.isFallFlying());
        state.setData(ClientRenderDataKeys.PET, renderData);
        state.setData(ClientRenderDataKeys.MOTION,
                new CarriedWolfMotionSnapshot(player.getId(), player.level().getGameTime(), input));
    }
}
