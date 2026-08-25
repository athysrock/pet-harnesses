package io.github.athysrock.petharnesses.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class InventoryExitPolicyTest {
    @Test
    void replacementUsesInventoryThenWorldDropOnOverflow() {
        assertEquals(HarnessDestination.REPLACED_LEGS_SLOT,
                InventoryExitPolicy.destination(false, true));
        assertEquals(HarnessDestination.INVENTORY,
                InventoryExitPolicy.destination(true, true));
        assertEquals(HarnessDestination.WORLD_DROP,
                InventoryExitPolicy.destination(true, false));
    }
}
