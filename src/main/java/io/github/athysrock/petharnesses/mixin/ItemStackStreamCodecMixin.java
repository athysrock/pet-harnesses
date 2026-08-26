package io.github.athysrock.petharnesses.mixin;

import io.github.athysrock.petharnesses.network.CapturedPetNetworkSanitizer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Filters the persistent-only captured entity component at the common ItemStack packet boundary. */
@Mixin(targets = "net.minecraft.world.item.ItemStack$1")
abstract class ItemStackStreamCodecMixin {
    @Redirect(
            method = "encode(Lnet/minecraft/network/RegistryFriendlyByteBuf;"
                    + "Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/component/PatchedDataComponentMap;"
                            + "asPatch()Lnet/minecraft/core/component/DataComponentPatch;"))
    private DataComponentPatch petHarnesses$stripCapturedPet(PatchedDataComponentMap components) {
        return CapturedPetNetworkSanitizer.forNetwork(components.asPatch());
    }
}
