package io.github.athysrock.petharnesses.domain;

import java.util.Objects;
import java.util.UUID;

public final class PetEligibility {
    private PetEligibility() {}

    public static Eligibility check(UUID actorId, PetFacts pet) {
        if (pet.species() != Species.CAT && pet.species() != Species.WOLF) return Eligibility.UNSUPPORTED_SPECIES;
        if (!pet.tamed()) return Eligibility.UNTAMED;
        if (!Objects.equals(actorId, pet.ownerId())) return Eligibility.NOT_OWNER;
        if (pet.alreadyLinked()) return Eligibility.ALREADY_LINKED;
        return Eligibility.ACCEPT;
    }
}
