package io.github.athysrock.petharnesses.item;

import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModAttachments;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.domain.LinkInteraction;
import io.github.athysrock.petharnesses.domain.LinkRecoveryPolicy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-authoritative link/recovery interaction. Capture remains delegated to the lifecycle seam. */
public final class HarnessItem extends Item {
    public HarnessItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Wolf pet))
            return InteractionResult.PASS;

        // The attachment is synchronized for rendering, but mutations remain server-authoritative.
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        if (!pet.isAlive() || pet.isBaby() || pet.isLeashed()
                || !pet.isTame() || !pet.isOwnedBy(player)) return InteractionResult.FAIL;

        HarnessLink itemLink = stack.get(ModComponents.HARNESS_LINK);
        HarnessLink petLink = pet.getAttached(ModAttachments.PET_LINK);
        LinkInteraction decision = LinkRecoveryPolicy.petInteraction(
                pet.isTame() && pet.isOwnedBy(player), itemLink, petLink,
                stack.has(ModComponents.CAPTURED_PET), player.getUUID(), pet.getUUID());
        if (decision == LinkInteraction.REJECT) return InteractionResult.FAIL;

        if (decision == LinkInteraction.ATTACH_NEW) {
            HarnessLink link = new HarnessLink(player.getUUID(), pet.getUUID());
            stack.set(ModComponents.HARNESS_LINK, link);
            pet.setAttached(ModAttachments.PET_LINK, link);
        } else {
            stack.remove(ModComponents.HARNESS_LINK);
            pet.removeAttached(ModAttachments.PET_LINK);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HarnessLink itemLink = stack.get(ModComponents.HARNESS_LINK);
        if (stack.has(ModComponents.CAPTURED_PET)
                || stack.has(ModComponents.PET_RENDER_DATA)
                || (itemLink != null && !itemLink.ownerId().equals(player.getUUID()))) {
            return InteractionResult.FAIL;
        }
        if (!player.isShiftKeyDown() || itemLink == null) return super.use(level, player, hand);
        if (!level.isClientSide()) {
            stack.remove(ModComponents.HARNESS_LINK);
        }
        return InteractionResult.SUCCESS;
    }
}
