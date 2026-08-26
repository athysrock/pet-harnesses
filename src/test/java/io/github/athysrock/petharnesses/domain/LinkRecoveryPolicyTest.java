package io.github.athysrock.petharnesses.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.github.athysrock.petharnesses.component.HarnessLink;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class LinkRecoveryPolicyTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OLD_PET = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void wholeHarnessRelinksOwnedPetWithStalePetLink() {
        assertEquals(LinkInteraction.ATTACH_NEW,
                LinkRecoveryPolicy.petInteraction(true, null, new HarnessLink(OWNER, OLD_PET), false, OWNER, PET));
    }

    @Test
    void occupiedPayloadWithoutItemLinkCannotRelinkPet() {
        assertEquals(LinkInteraction.REJECT,
                LinkRecoveryPolicy.petInteraction(true, null, new HarnessLink(OWNER, OLD_PET), true, OWNER, PET));
    }

    @Test
    void duplicateOldItemCannotDetachPetAfterNewIdentityReplacesStaleLink() {
        HarnessLink old = new HarnessLink(OWNER, PET);
        HarnessLink replacement = new HarnessLink(OWNER, PET);
        assertNotSame(old, replacement);
        assertEquals(LinkInteraction.REJECT,
                LinkRecoveryPolicy.petInteraction(true, old, replacement, false, OWNER, PET));
    }
}
