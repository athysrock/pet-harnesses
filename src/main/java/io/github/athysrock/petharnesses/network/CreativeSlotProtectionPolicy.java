package io.github.athysrock.petharnesses.network;

import io.github.athysrock.petharnesses.component.CapturedPetData;
import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.component.PetRenderData;
import io.github.athysrock.petharnesses.item.ModItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/** Fail-closed policy for vanilla creative slot packets carrying server-projected item stacks. */
public final class CreativeSlotProtectionPolicy {
    private CreativeSlotProtectionPolicy() {}

    public static boolean reject(ItemStack currentSlot, ItemStack submitted, List<ItemStack> occupiedStacks) {
        return reject(facts(currentSlot), facts(submitted), occupiedStacks.stream()
                .map(CreativeSlotProtectionPolicy::facts).toList());
    }

    public static boolean reject(StackFacts currentSlot, StackFacts submitted, List<StackFacts> occupiedStacks) {
        if (currentSlot.occupied() || submitted.capturedPayload()) return true;
        if (submitted.link() == null && submitted.renderPetId() == null) return false;
        return occupiedStacks.stream()
                .filter(StackFacts::occupied)
                .anyMatch(occupied -> occupied.link().equals(submitted.link())
                        || occupied.link().petId().equals(submitted.renderPetId()));
    }

    public static boolean isOccupied(ItemStack stack) {
        return facts(stack).occupied();
    }

    private static StackFacts facts(ItemStack stack) {
        if (!stack.is(ModItems.PET_HARNESS)) return new StackFacts(false, null, null, false);
        HarnessLink link = stack.get(ModComponents.HARNESS_LINK);
        CapturedPetData captured = stack.get(ModComponents.CAPTURED_PET);
        PetRenderData render = stack.get(ModComponents.PET_RENDER_DATA);
        boolean occupied = link != null && captured != null && render != null
                && link.ownerId().equals(captured.ownerId())
                && link.petId().equals(captured.petId())
                && link.petId().equals(render.petId());
        return new StackFacts(occupied, link, render == null ? null : render.petId(), captured != null);
    }

    public record StackFacts(boolean occupied, HarnessLink link, UUID renderPetId, boolean capturedPayload) {}
}
