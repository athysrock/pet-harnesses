package io.github.athysrock.petharnesses.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

final class HarnessTransactionsTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void captureSerializesAndCommitsBeforeRemovingLivePet() {
        HarnessState linked = HarnessState.linked(OWNER, PET);
        byte[] completeEntityData = "{Health:7.5f,variant:'tabby',ArmorItems:[{id:'wolf_armor'}],modded:{x:42}}"
                .getBytes(StandardCharsets.UTF_8);
        List<String> calls = new ArrayList<>();
        CapturePort port = capturePort(calls, true, true,
                CaptureRollbackOutcome.LINKED_AND_MATCHING_LIVE_CONFIRMED,
                Optional.of(new CapturedPet(1, PET, OWNER, Species.WOLF, completeEntityData)));

        TransitionResult result = HarnessTransactions.capture(linked, port);

        assertTrue(result.success());
        assertEquals(HarnessPhase.OCCUPIED, result.state().phase());
        assertArrayEquals(completeEntityData, result.state().capturedPet().entityData());
        assertEquals(List.of("serialize", "commit", "remove"), calls);
    }

    @Test
    void failedSerializationNeverCommitsOrRemoves() {
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.capture(HarnessState.linked(OWNER, PET),
                capturePort(calls, true, true,
                        CaptureRollbackOutcome.LINKED_AND_MATCHING_LIVE_CONFIRMED, Optional.empty()));
        assertEquals(TransitionError.SERIALIZATION_FAILED, result.error());
        assertEquals(List.of("serialize"), calls);
    }

    @Test
    void removalFailureWithDurableLinkedRollbackAndMatchingLivePetReportsSafeLinkedFailure() {
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.capture(HarnessState.linked(OWNER, PET),
                capturePort(calls, true, false,
                        CaptureRollbackOutcome.LINKED_AND_MATCHING_LIVE_CONFIRMED, captured()));
        assertFalse(result.success());
        assertEquals(TransitionError.REMOVAL_FAILED, result.error());
        assertEquals(HarnessPhase.LINKED, result.state().phase());
        assertEquals(List.of("serialize", "commit", "remove", "rollback"), calls);
    }

    @Test
    void removalFailureWithAbsentLivePetRetainsOccupiedForReconciliation() {
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.capture(HarnessState.linked(OWNER, PET),
                capturePort(calls, true, false, CaptureRollbackOutcome.LIVE_ABSENT, captured()));
        assertFalse(result.success());
        assertEquals(TransitionError.ROLLBACK_FAILED_RECONCILIATION_REQUIRED, result.error());
        assertEquals(HarnessPhase.OCCUPIED, result.state().phase());
        assertEquals(captured().orElseThrow(), result.state().capturedPet());
        assertEquals(List.of("serialize", "commit", "remove", "rollback"), calls);
    }

    @Test
    void removalFailureWithUnknownLivePresenceRetainsOccupiedForReconciliation() {
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.capture(HarnessState.linked(OWNER, PET),
                capturePort(calls, true, false, CaptureRollbackOutcome.LIVE_PRESENCE_UNKNOWN, captured()));
        assertFalse(result.success());
        assertEquals(TransitionError.ROLLBACK_FAILED_RECONCILIATION_REQUIRED, result.error());
        assertEquals(HarnessPhase.OCCUPIED, result.state().phase());
        assertEquals(captured().orElseThrow(), result.state().capturedPet());
        assertEquals(List.of("serialize", "commit", "remove", "rollback"), calls);
    }

    @Test
    void removalFailureWithNonDurableLinkedMutationRetainsOccupiedForReconciliation() {
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.capture(HarnessState.linked(OWNER, PET),
                capturePort(calls, true, false, CaptureRollbackOutcome.LINKED_MUTATION_NOT_DURABLE, captured()));
        assertFalse(result.success());
        assertEquals(TransitionError.ROLLBACK_FAILED_RECONCILIATION_REQUIRED, result.error());
        assertEquals(HarnessPhase.OCCUPIED, result.state().phase());
        assertEquals(captured().orElseThrow(), result.state().capturedPet());
        assertEquals(List.of("serialize", "commit", "remove", "rollback"), calls);
    }

    @Test
    void restoreInsertsOriginalUuidBeforeClearingCapturedData() {
        HarnessState occupied = occupied();
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.restore(occupied,
                restorePort(calls, DurablePetPresence.ABSENT, true, true, true));
        assertTrue(result.success());
        assertEquals(HarnessPhase.LINKED, result.state().phase());
        assertEquals(List.of("inspect", "reconstruct", "insert", "clear"), calls);
    }

    @Test
    void insertionSuccessAndClearFailureReportsOccupiedReconciliationRisk() {
        HarnessState occupied = occupied();
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.restore(occupied,
                restorePort(calls, DurablePetPresence.ABSENT, true, true, false));
        assertFalse(result.success());
        assertEquals(TransitionError.CLEAR_FAILED_RECONCILIATION_REQUIRED, result.error());
        assertEquals(occupied, result.state());
        assertEquals(List.of("inspect", "reconstruct", "insert", "clear"), calls);
    }

    @Test
    void retryAfterInsertionCanVerifyMatchingPetAndOnlyRetryClear() {
        HarnessState occupied = occupied();
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.restore(occupied,
                restorePort(calls, DurablePetPresence.MATCHING_RESTORED, true, true, true));
        assertTrue(result.success());
        assertEquals(HarnessPhase.LINKED, result.state().phase());
        assertEquals(List.of("inspect", "clear"), calls);
    }

    @Test
    void conflictingUuidRestoreFailsClosedWithoutReconstructingOrClearing() {
        HarnessState occupied = occupied();
        List<String> calls = new ArrayList<>();
        TransitionResult result = HarnessTransactions.restore(occupied,
                restorePort(calls, DurablePetPresence.CONFLICT, true, true, true));
        assertEquals(TransitionError.DUPLICATE_UUID, result.error());
        assertEquals(occupied, result.state());
        assertEquals(List.of("inspect"), calls);
    }

    private static Optional<CapturedPet> captured() {
        return Optional.of(new CapturedPet(1, PET, OWNER, Species.WOLF, new byte[] {9}));
    }

    private static HarnessState occupied() {
        return HarnessState.linked(OWNER, PET).occupied(captured().orElseThrow());
    }

    private static CapturePort capturePort(List<String> calls, boolean commit, boolean remove,
                                           CaptureRollbackOutcome rollback, Optional<CapturedPet> serialized) {
        return new CapturePort() {
            @Override public Optional<CapturedPet> serialize(UUID petId) { calls.add("serialize"); return serialized; }
            @Override public boolean commitOccupied(HarnessState occupied) { calls.add("commit"); return commit; }
            @Override public boolean removeLive(UUID petId) { calls.add("remove"); return remove; }
            @Override public CaptureRollbackOutcome rollbackOccupiedIfMatchingLivePresent(
                    HarnessState occupied, HarnessState linkedState) {
                calls.add("rollback");
                return rollback;
            }
        };
    }

    private static RestorePort restorePort(List<String> calls, DurablePetPresence presence,
                                           boolean reconstruct, boolean insert, boolean clear) {
        return new RestorePort() {
            @Override public DurablePetPresence inspect(UUID petId, CapturedPet captured) { calls.add("inspect"); return presence; }
            @Override public boolean reconstruct(CapturedPet captured) { calls.add("reconstruct"); return reconstruct; }
            @Override public boolean insertWithOriginalUuid(UUID petId) { calls.add("insert"); return insert; }
            @Override public boolean clearCaptured(HarnessState linked) { calls.add("clear"); return clear; }
        };
    }
}
