package io.github.athysrock.petharnesses.domain;

/**
 * Durable result of an attempted capture rollback after live removal was not confirmed.
 * Only {@link #LINKED_AND_MATCHING_LIVE_CONFIRMED} permits the captured payload to be cleared.
 * Every other result requires retaining the OCCUPIED payload and reconciliation.
 */
public enum CaptureRollbackOutcome {
    /** Both LINKED item state and the matching live pet are durably readable after rollback. */
    LINKED_AND_MATCHING_LIVE_CONFIRMED,
    /** The matching live pet is durably absent, so the captured payload must be retained. */
    LIVE_ABSENT,
    /** Matching live-pet presence cannot be established, so the captured payload must be retained. */
    LIVE_PRESENCE_UNKNOWN,
    /** The LINKED mutation was not durably confirmed, so the captured payload must be retained. */
    LINKED_MUTATION_NOT_DURABLE
}
