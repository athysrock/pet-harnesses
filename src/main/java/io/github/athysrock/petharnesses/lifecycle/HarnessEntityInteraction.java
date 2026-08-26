package io.github.athysrock.petharnesses.lifecycle;

import io.github.athysrock.petharnesses.item.ModItems;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;

/** Routes the harness before vanilla owner-wolf interaction consumes the click as a sit toggle. */
public final class HarnessEntityInteraction {
    private HarnessEntityInteraction() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isSpectator() || !stack.is(ModItems.PET_HARNESS)
                    || !(entity instanceof Wolf wolf)) {
                return InteractionResult.PASS;
            }
            return stack.interactLivingEntity(player, wolf, hand);
        });
    }
}
