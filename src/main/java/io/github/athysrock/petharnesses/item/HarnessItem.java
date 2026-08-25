package io.github.athysrock.petharnesses.item;

import io.github.athysrock.petharnesses.PetHarnesses;
import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModAttachments;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.domain.LinkIdentity;
import io.github.athysrock.petharnesses.domain.LinkInteraction;
import io.github.athysrock.petharnesses.domain.LinkRecoveryPolicy;
import io.github.athysrock.petharnesses.domain.LinkedItemReset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-authoritative link/recovery interaction. Capture remains delegated to the lifecycle seam. */
public final class HarnessItem extends Item {
    private static final Identifier WHOLE_MODEL = PetHarnesses.id("pet_harness");
    private static final Identifier LINKED_MODEL = PetHarnesses.id("pet_harness_linked");

    public HarnessItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof TamableAnimal pet) || (!(pet instanceof Cat) && !(pet instanceof Wolf)))
            return InteractionResult.PASS;

        // PET_LINK is persistent but intentionally unsynchronized. Prediction must never consult it.
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        HarnessLink itemLink = stack.get(ModComponents.HARNESS_LINK);
        HarnessLink petLink = pet.getAttached(ModAttachments.PET_LINK);
        LinkInteraction decision = LinkRecoveryPolicy.petInteraction(
                pet.isTame() && pet.isOwnedBy(player), identity(itemLink), identity(petLink),
                stack.has(ModComponents.CAPTURED_PET), player.getUUID(), pet.getUUID());
        if (decision == LinkInteraction.REJECT) return InteractionResult.FAIL;

        if (decision == LinkInteraction.ATTACH_NEW) {
            HarnessLink link = new HarnessLink(player.getUUID(), pet.getUUID());
            stack.set(ModComponents.HARNESS_LINK, link);
            stack.set(DataComponents.ITEM_MODEL, LINKED_MODEL);
            pet.setAttached(ModAttachments.PET_LINK, link);
        } else {
            stack.remove(ModComponents.HARNESS_LINK);
            stack.set(DataComponents.ITEM_MODEL, WHOLE_MODEL);
            pet.removeAttached(ModAttachments.PET_LINK);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HarnessLink itemLink = stack.get(ModComponents.HARNESS_LINK);
        LinkedItemReset decision = LinkRecoveryPolicy.linkedItemReset(
                player.isShiftKeyDown(), identity(itemLink), stack.has(ModComponents.CAPTURED_PET), player.getUUID());
        if (decision != LinkedItemReset.RESET) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            stack.remove(ModComponents.HARNESS_LINK);
            stack.set(DataComponents.ITEM_MODEL, WHOLE_MODEL);
        }
        return InteractionResult.SUCCESS;
    }

    private static LinkIdentity identity(HarnessLink link) {
        return link == null ? null : new LinkIdentity(link.linkId(), link.ownerId(), link.petId());
    }
}
