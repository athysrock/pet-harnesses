package io.github.athysrock.petharnesses.domain;

import java.util.Optional;
import java.util.UUID;

public interface CapturePort {
    Optional<CapturedPet> serialize(UUID petId);
    /** True only when the OCCUPIED state is durably readable; repeated calls must accept that state. */
    boolean commitOccupied(HarnessState occupied);
    /** True only when the matching live pet is durably absent; false includes every ambiguous outcome. */
    boolean removeLive(UUID petId);
    /**
     * Attempts to replace {@code occupiedState} with {@code linkedState} after removal was not confirmed.
     * The implementation must clear the captured payload only when a post-mutation durable read confirms
     * BOTH the exact LINKED item state and durable presence of the live pet matching the occupied payload.
     * If matching presence is absent or unknown, or the LINKED mutation is not durable, it must retain (or
     * restore) OCCUPIED and return the corresponding reconciliation outcome. The operation must be retry-safe.
     */
    CaptureRollbackOutcome rollbackOccupiedIfMatchingLivePresent(
            HarnessState occupiedState, HarnessState linkedState);
}
