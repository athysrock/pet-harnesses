package io.github.athysrock.petharnesses.network;

import java.util.List;
import io.github.athysrock.petharnesses.lifecycle.HarnessStackLocator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Resolves the vanilla creative packet's real inventory-menu slot against global server authority. */
public final class CreativeSlotProtection {
    private CreativeSlotProtection() {}

    public static boolean reject(ServerPlayer sender, int slotNumber, ItemStack submitted) {
        ItemStack current = slotNumber >= 1 && slotNumber <= 45
                ? sender.inventoryMenu.getSlot(slotNumber).getItem()
                : ItemStack.EMPTY;
        return CreativeSlotProtectionPolicy.reject(
                current, submitted, HarnessStackLocator.allOccupied(sender.level().getServer()).stream()
                        .map(HarnessStackLocator.LocatedHarness::stack).toList());
    }
}
