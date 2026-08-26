package io.github.athysrock.petharnesses.domain;

import io.github.athysrock.petharnesses.component.HarnessLink;
import java.util.UUID;

/** Pure policy shared by server interaction code and focused recovery tests. */
public final class LinkRecoveryPolicy {
    private LinkRecoveryPolicy() {}

    public static LinkInteraction petInteraction(boolean owned, HarnessLink itemLink, HarnessLink petLink,
                                                 boolean hasCapturedPayload, UUID playerId, UUID petId) {
        if (!owned || hasCapturedPayload) return LinkInteraction.REJECT;
        if (itemLink == null) return LinkInteraction.ATTACH_NEW;
        boolean exact = itemLink.equals(petLink)
                && itemLink.ownerId().equals(playerId)
                && itemLink.petId().equals(petId);
        return exact ? LinkInteraction.DETACH : LinkInteraction.REJECT;
    }

}
