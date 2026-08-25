package io.github.athysrock.petharnesses.domain;

import java.util.Optional;

public final class HarnessTransactions {
    private HarnessTransactions() {}

    public static TransitionResult capture(HarnessState state, CapturePort port) {
        if (state.phase() != HarnessPhase.LINKED) return TransitionResult.failure(state, TransitionError.INVALID_PHASE);
        Optional<CapturedPet> serialized = port.serialize(state.petId());
        if (serialized.isEmpty()) return TransitionResult.failure(state, TransitionError.SERIALIZATION_FAILED);
        CapturedPet pet = serialized.get();
        if (!state.petId().equals(pet.petId()) || !state.ownerId().equals(pet.ownerId()))
            return TransitionResult.failure(state, TransitionError.IDENTITY_MISMATCH);
        HarnessState occupied = state.occupied(pet);
        if (!port.commitOccupied(occupied)) return TransitionResult.failure(state, TransitionError.COMMIT_FAILED);
        if (!port.removeLive(state.petId())) {
            CaptureRollbackOutcome rollback =
                    port.rollbackOccupiedIfMatchingLivePresent(occupied, state);
            if (rollback == CaptureRollbackOutcome.LINKED_AND_MATCHING_LIVE_CONFIRMED)
                return TransitionResult.failure(state, TransitionError.REMOVAL_FAILED);
            return TransitionResult.failure(occupied, TransitionError.ROLLBACK_FAILED_RECONCILIATION_REQUIRED);
        }
        return TransitionResult.success(occupied);
    }

    public static TransitionResult restore(HarnessState state, RestorePort port) {
        if (state.phase() != HarnessPhase.OCCUPIED) return TransitionResult.failure(state, TransitionError.INVALID_PHASE);
        DurablePetPresence presence = port.inspect(state.petId(), state.capturedPet());
        if (presence == DurablePetPresence.CONFLICT)
            return TransitionResult.failure(state, TransitionError.DUPLICATE_UUID);
        if (presence == DurablePetPresence.ABSENT) {
            if (!port.reconstruct(state.capturedPet())) return TransitionResult.failure(state, TransitionError.RESTORE_FAILED);
            if (!port.insertWithOriginalUuid(state.petId())) return TransitionResult.failure(state, TransitionError.RESTORE_FAILED);
        }
        HarnessState linked = state.linkedAgain();
        if (!port.clearCaptured(linked))
            return TransitionResult.failure(state, TransitionError.CLEAR_FAILED_RECONCILIATION_REQUIRED);
        return TransitionResult.success(linked);
    }
}
