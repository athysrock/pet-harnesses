package io.github.athysrock.petharnesses.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class HarnessTransitionsTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void attachCreatesLinkedHumanHalfAndDetachRestoresWholeHarness() {
        HarnessState whole = HarnessState.whole();
        TransitionResult attached = HarnessTransitions.attach(whole, OWNER, PET);

        assertTrue(attached.success());
        assertEquals(HarnessPhase.LINKED, attached.state().phase());
        assertEquals(OWNER, attached.state().ownerId());
        assertEquals(PET, attached.state().petId());
        assertNull(attached.state().capturedPet());

        TransitionResult detached = HarnessTransitions.detach(attached.state(), OWNER, PET);
        assertTrue(detached.success());
        assertEquals(HarnessState.whole(), detached.state());
    }

    @Test
    void detachRejectsDifferentPetOrOwner() {
        HarnessState linked = HarnessTransitions.attach(HarnessState.whole(), OWNER, PET).state();
        assertEquals(TransitionError.IDENTITY_MISMATCH,
                HarnessTransitions.detach(linked, OWNER, UUID.randomUUID()).error());
        assertEquals(TransitionError.IDENTITY_MISMATCH,
                HarnessTransitions.detach(linked, UUID.randomUUID(), PET).error());
    }
}
