package io.github.athysrock.petharnesses.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class HarnessStateIdentityTest {
    @Test
    void capturedPetRejectsFutureDomainSchema() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID pet = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertThrows(IllegalArgumentException.class, () -> new CapturedPet(
                CapturedPet.CURRENT_SCHEMA + 1, pet, owner, Species.CAT, new byte[] {1}));
    }

    @Test
    void occupiedRejectsCapturedPetWhoseIdentityDiffersFromLink() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID pet = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID otherPet = UUID.fromString("00000000-0000-0000-0000-000000000003");
        assertThrows(IllegalArgumentException.class, () -> HarnessState.linked(owner, pet)
                .occupied(new CapturedPet(1, otherPet, owner, Species.CAT, new byte[] {1})));
    }
}
