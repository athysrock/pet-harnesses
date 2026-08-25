package io.github.athysrock.petharnesses.domain;

import java.util.UUID;

/** Pure policy shared by server interaction code and focused recovery tests. */
public final class LinkRecoveryPolicy {
    private LinkRecoveryPolicy() {}

    public static LinkInteraction petInteraction(boolean owned, LinkIdentity itemLink, LinkIdentity petLink,
                                                 boolean hasCapturedPayload, UUID playerId, UUID petId) {
        if (!owned || hasCapturedPayload) return LinkInteraction.REJECT;
        if (itemLink == null) return LinkInteraction.ATTACH_NEW;
        boolean exact = itemLink.equals(petLink)
                && itemLink.ownerId().equals(playerId)
                && itemLink.petId().equals(petId);
        return exact ? LinkInteraction.DETACH : LinkInteraction.REJECT;
    }

    public static LinkedItemReset linkedItemReset(boolean sneaking, LinkIdentity itemLink,
                                                   boolean hasCapturedPayload, UUID playerId) {
        if (!sneaking) return LinkedItemReset.REJECT_NOT_SNEAKING;
        if (itemLink == null) return LinkedItemReset.REJECT_NOT_LINKED;
        if (hasCapturedPayload) return LinkedItemReset.REJECT_CAPTURED_PAYLOAD;
        if (!itemLink.ownerId().equals(playerId)) return LinkedItemReset.REJECT_OWNER;
        return LinkedItemReset.RESET;
    }
}
