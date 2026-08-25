package io.github.athysrock.petharnesses.domain;

import java.util.UUID;

/** Adapter boundary for reconstructing a captured entity without creating a generic fallback. */
public interface RestorePort {
    /** Distinguishes a prior successful insertion from an unrelated UUID conflict. */
    DurablePetPresence inspect(UUID petId, CapturedPet captured);
    boolean reconstruct(CapturedPet captured);
    /** True only when this captured pet is durably present; repeated calls must accept that state. */
    boolean insertWithOriginalUuid(UUID petId);
    /** True only when LINKED without captured data is durably readable; repeated calls must accept that state. */
    boolean clearCaptured(HarnessState linkedState);
}
