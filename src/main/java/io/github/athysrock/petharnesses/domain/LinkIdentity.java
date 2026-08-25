package io.github.athysrock.petharnesses.domain;

import java.util.Objects;
import java.util.UUID;

public record LinkIdentity(UUID linkId, UUID ownerId, UUID petId) {
    public LinkIdentity {
        Objects.requireNonNull(linkId);
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(petId);
    }

    public LinkIdentity(UUID ownerId, UUID petId) { this(UUID.randomUUID(), ownerId, petId); }
}
