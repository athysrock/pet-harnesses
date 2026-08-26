package io.github.athysrock.petharnesses.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.athysrock.petharnesses.component.HarnessLink;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CreativeSlotProtectionPolicyTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LINK = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final HarnessLink HARNESS_LINK =
            new HarnessLink(HarnessLink.CURRENT_VERSION, LINK, OWNER, PET);

    @Test
    void rejectsReplacementOrRemovalOfAuthoritativeOccupiedSlot() {
        var occupied = occupied();

        assertTrue(CreativeSlotProtectionPolicy.reject(occupied, empty(), List.of(occupied)));
    }

    @Test
    void rejectsCopyOfOccupiedClientProjectionIntoAnotherSlot() {
        var projection = new CreativeSlotProtectionPolicy.StackFacts(false, HARNESS_LINK, PET, false);

        assertTrue(CreativeSlotProtectionPolicy.reject(empty(), projection, List.of(occupied())));
    }

    @Test
    void rejectsAnyInboundCapturedPayloadAndPermitsUnrelatedEdit() {
        var hostilePayload = new CreativeSlotProtectionPolicy.StackFacts(false, null, null, true);

        assertTrue(CreativeSlotProtectionPolicy.reject(empty(), hostilePayload, List.of(occupied())));
        assertFalse(CreativeSlotProtectionPolicy.reject(empty(), empty(), List.of(occupied())));
    }

    private static CreativeSlotProtectionPolicy.StackFacts occupied() {
        return new CreativeSlotProtectionPolicy.StackFacts(true, HARNESS_LINK, PET, true);
    }

    private static CreativeSlotProtectionPolicy.StackFacts empty() {
        return new CreativeSlotProtectionPolicy.StackFacts(false, null, null, false);
    }
}
