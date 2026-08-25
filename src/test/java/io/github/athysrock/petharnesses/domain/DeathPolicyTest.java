package io.github.athysrock.petharnesses.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

final class DeathPolicyTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final HarnessState OCCUPIED = HarnessState.linked(OWNER, PET)
            .occupied(new CapturedPet(1, PET, OWNER, Species.CAT, new byte[] {1, 2, 3}));

    @Test
    void keepInventoryRetainsOccupiedHarnessWithoutMaterializingPet() {
        List<String> calls = new ArrayList<>();
        DeathOutcome outcome = HarnessDeathPolicy.onDeath(OCCUPIED, true, recordingSuccessfulPort(calls));
        assertEquals(DeathDisposition.RETAIN_OCCUPIED, outcome.disposition());
        assertEquals(OCCUPIED, outcome.state());
        assertTrue(calls.isEmpty());
    }

    @Test
    void ordinaryDeathMaterializesPetThenDropsOnlyLinkedNonOccupiedHarness() {
        List<String> calls = new ArrayList<>();
        DeathOutcome outcome = HarnessDeathPolicy.onDeath(OCCUPIED, false, recordingSuccessfulPort(calls));
        assertEquals(DeathDisposition.DROP_LINKED, outcome.disposition());
        assertEquals(HarnessPhase.LINKED, outcome.state().phase());
        assertNull(outcome.state().capturedPet());
        assertEquals(List.of("inspect", "reconstruct", "insert", "clear"), calls);
    }

    @Test
    void wholeInputIsUnchangedAndNeverCallsRestore() {
        List<String> calls = new ArrayList<>();
        DeathOutcome outcome = HarnessDeathPolicy.onDeath(HarnessState.whole(), false, recordingSuccessfulPort(calls));
        assertEquals(DeathDisposition.UNCHANGED, outcome.disposition());
        assertEquals(HarnessState.whole(), outcome.state());
        assertTrue(calls.isEmpty());
    }

    @Test
    void linkedInputIsUnchangedAndNeverCallsRestore() {
        List<String> calls = new ArrayList<>();
        HarnessState linked = HarnessState.linked(OWNER, PET);
        DeathOutcome outcome = HarnessDeathPolicy.onDeath(linked, false, recordingSuccessfulPort(calls));
        assertEquals(DeathDisposition.UNCHANGED, outcome.disposition());
        assertEquals(linked, outcome.state());
        assertTrue(calls.isEmpty());
    }

    private static RestorePort recordingSuccessfulPort(List<String> calls) {
        return new RestorePort() {
            @Override public DurablePetPresence inspect(UUID petId, CapturedPet captured) { calls.add("inspect"); return DurablePetPresence.ABSENT; }
            @Override public boolean reconstruct(CapturedPet captured) { calls.add("reconstruct"); return true; }
            @Override public boolean insertWithOriginalUuid(UUID petId) { calls.add("insert"); return true; }
            @Override public boolean clearCaptured(HarnessState linkedState) { calls.add("clear"); return true; }
        };
    }
}
