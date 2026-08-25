package io.github.athysrock.petharnesses.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class LinkRecoveryPolicyTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OLD_PET = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void wholeHarnessRelinksOwnedPetWithStalePetLink() {
        assertEquals(LinkInteraction.ATTACH_NEW,
                LinkRecoveryPolicy.petInteraction(true, null, new LinkIdentity(OWNER, OLD_PET), false, OWNER, PET));
    }

    @Test
    void staleLinkedItemCanBeResetByOwnerWithoutCapturedPayload() {
        assertEquals(LinkedItemReset.RESET,
                LinkRecoveryPolicy.linkedItemReset(true, new LinkIdentity(OWNER, OLD_PET), false, OWNER));
    }

    @Test
    void staleLinkedItemResetRejectsDifferentOwner() {
        UUID stranger = UUID.fromString("00000000-0000-0000-0000-000000000004");
        assertEquals(LinkedItemReset.REJECT_OWNER,
                LinkRecoveryPolicy.linkedItemReset(true, new LinkIdentity(OWNER, OLD_PET), false, stranger));
    }

    @Test
    void occupiedItemResetIsRejected() {
        assertEquals(LinkedItemReset.REJECT_CAPTURED_PAYLOAD,
                LinkRecoveryPolicy.linkedItemReset(true, new LinkIdentity(OWNER, OLD_PET), true, OWNER));
    }

    @Test
    void occupiedPayloadWithoutItemLinkCannotRelinkPet() {
        assertEquals(LinkInteraction.REJECT,
                LinkRecoveryPolicy.petInteraction(true, null, new LinkIdentity(OWNER, OLD_PET), true, OWNER, PET));
    }

    @Test
    void duplicateOldItemCannotDetachPetAfterNewIdentityReplacesStaleLink() {
        LinkIdentity old = new LinkIdentity(OWNER, PET);
        LinkIdentity replacement = new LinkIdentity(OWNER, PET);
        assertNotSame(old, replacement);
        assertEquals(LinkInteraction.REJECT,
                LinkRecoveryPolicy.petInteraction(true, old, replacement, false, OWNER, PET));
    }
}
