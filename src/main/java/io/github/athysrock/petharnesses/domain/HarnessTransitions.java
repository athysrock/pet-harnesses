package io.github.athysrock.petharnesses.domain;

import java.util.Objects;
import java.util.UUID;

public final class HarnessTransitions {
    private HarnessTransitions() {}

    public static TransitionResult attach(HarnessState state, UUID ownerId, UUID petId) {
        if (state.phase() != HarnessPhase.WHOLE) return TransitionResult.failure(state, TransitionError.INVALID_PHASE);
        return TransitionResult.success(HarnessState.linked(Objects.requireNonNull(ownerId), Objects.requireNonNull(petId)));
    }

    public static TransitionResult detach(HarnessState state, UUID ownerId, UUID petId) {
        if (state.phase() != HarnessPhase.LINKED) return TransitionResult.failure(state, TransitionError.INVALID_PHASE);
        if (!state.ownerId().equals(ownerId) || !state.petId().equals(petId))
            return TransitionResult.failure(state, TransitionError.IDENTITY_MISMATCH);
        return TransitionResult.success(HarnessState.whole());
    }
}
