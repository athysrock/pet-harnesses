package io.github.athysrock.petharnesses.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/** Versioned, opaque complete entity payload plus redundant identity metadata. */
public final class CapturedPet {
    public static final int CURRENT_SCHEMA = 1;
    private final int schemaVersion;
    private final UUID petId;
    private final UUID ownerId;
    private final Species species;
    private final byte[] entityData;

    public CapturedPet(int schemaVersion, UUID petId, UUID ownerId, Species species, byte[] entityData) {
        if (schemaVersion != CURRENT_SCHEMA) throw new IllegalArgumentException("schemaVersion");
        this.schemaVersion = schemaVersion;
        this.petId = Objects.requireNonNull(petId);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.species = Objects.requireNonNull(species);
        this.entityData = Objects.requireNonNull(entityData).clone();
    }

    public int schemaVersion() { return schemaVersion; }
    public UUID petId() { return petId; }
    public UUID ownerId() { return ownerId; }
    public Species species() { return species; }
    public byte[] entityData() { return entityData.clone(); }

    @Override public boolean equals(Object value) {
        return value instanceof CapturedPet other && schemaVersion == other.schemaVersion
                && petId.equals(other.petId) && ownerId.equals(other.ownerId)
                && species == other.species && Arrays.equals(entityData, other.entityData);
    }

    @Override public int hashCode() {
        return 31 * Objects.hash(schemaVersion, petId, ownerId, species) + Arrays.hashCode(entityData);
    }
}
