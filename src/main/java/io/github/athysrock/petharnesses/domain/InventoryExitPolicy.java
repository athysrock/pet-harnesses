package io.github.athysrock.petharnesses.domain;

public final class InventoryExitPolicy {
    private InventoryExitPolicy() {}

    public static HarnessDestination destination(boolean slotWasReplaced, boolean inventoryHasSpace) {
        if (!slotWasReplaced) return HarnessDestination.REPLACED_LEGS_SLOT;
        return inventoryHasSpace ? HarnessDestination.INVENTORY : HarnessDestination.WORLD_DROP;
    }
}
