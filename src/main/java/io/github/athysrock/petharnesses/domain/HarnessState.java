package io.github.athysrock.petharnesses.domain;

import java.util.Objects;
import java.util.UUID;

public record HarnessState(int schemaVersion, HarnessPhase phase, UUID ownerId, UUID petId, CapturedPet capturedPet) {
    public static final int CURRENT_SCHEMA = 1;

    public HarnessState {
        Objects.requireNonNull(phase);
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA) throw new IllegalArgumentException("schemaVersion");
        if (phase == HarnessPhase.WHOLE && (ownerId != null || petId != null || capturedPet != null))
            throw new IllegalArgumentException("whole harness cannot carry link state");
        if (phase != HarnessPhase.WHOLE && (ownerId == null || petId == null))
            throw new IllegalArgumentException("linked harness requires identities");
        if ((phase == HarnessPhase.OCCUPIED) != (capturedPet != null))
            throw new IllegalArgumentException("captured payload must exist only while occupied");
        if (capturedPet != null && (!ownerId.equals(capturedPet.ownerId()) || !petId.equals(capturedPet.petId())))
            throw new IllegalArgumentException("captured payload identity must match link");
    }

    public static HarnessState whole() { return new HarnessState(CURRENT_SCHEMA, HarnessPhase.WHOLE, null, null, null); }
    public static HarnessState linked(UUID ownerId, UUID petId) { return new HarnessState(CURRENT_SCHEMA, HarnessPhase.LINKED, ownerId, petId, null); }
    public HarnessState occupied(CapturedPet pet) { return new HarnessState(schemaVersion, HarnessPhase.OCCUPIED, ownerId, petId, pet); }
    public HarnessState linkedAgain() { return linked(ownerId, petId); }
}
