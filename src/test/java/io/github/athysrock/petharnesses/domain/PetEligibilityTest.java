package io.github.athysrock.petharnesses.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PetEligibilityTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void acceptsOnlyTamedCatOrWolfOwnedByActor() {
        assertEquals(Eligibility.ACCEPT, PetEligibility.check(OWNER, new PetFacts(Species.CAT, true, OWNER, false)));
        assertEquals(Eligibility.ACCEPT, PetEligibility.check(OWNER, new PetFacts(Species.WOLF, true, OWNER, false)));
        assertEquals(Eligibility.UNSUPPORTED_SPECIES, PetEligibility.check(OWNER, new PetFacts(Species.OTHER, true, OWNER, false)));
        assertEquals(Eligibility.UNTAMED, PetEligibility.check(OWNER, new PetFacts(Species.CAT, false, null, false)));
        assertEquals(Eligibility.NOT_OWNER, PetEligibility.check(UUID.randomUUID(), new PetFacts(Species.WOLF, true, OWNER, false)));
        assertEquals(Eligibility.ALREADY_LINKED, PetEligibility.check(OWNER, new PetFacts(Species.CAT, true, OWNER, true)));
    }
}
