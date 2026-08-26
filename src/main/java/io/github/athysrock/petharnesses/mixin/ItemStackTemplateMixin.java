package io.github.athysrock.petharnesses.mixin;

import io.github.athysrock.petharnesses.network.CapturedPetNetworkSanitizer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Makes every presentation-only item template incapable of retaining captured entity authority. */
@Mixin(ItemStackTemplate.class)
abstract class ItemStackTemplateMixin {
    @ModifyVariable(
            method = "<init>(Lnet/minecraft/core/Holder;I"
                    + "Lnet/minecraft/core/component/DataComponentPatch;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private static DataComponentPatch petHarnesses$stripCapturedPet(DataComponentPatch components) {
        return CapturedPetNetworkSanitizer.forNetwork(components);
    }
}
