package io.github.athysrock.petharnesses.mixin;

import io.github.athysrock.petharnesses.network.CapturedPetNetworkSanitizer;
import net.minecraft.core.component.DataComponentPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Filters both vanilla direct patch stream codecs at their shared concrete encoder. */
@Mixin(targets = "net.minecraft.core.component.DataComponentPatch$3")
abstract class DataComponentPatchStreamCodecMixin {
    @ModifyVariable(
            method = "encode(Lnet/minecraft/network/RegistryFriendlyByteBuf;"
                    + "Lnet/minecraft/core/component/DataComponentPatch;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private DataComponentPatch petHarnesses$stripCapturedPet(DataComponentPatch patch) {
        return CapturedPetNetworkSanitizer.forNetwork(patch);
    }
}
