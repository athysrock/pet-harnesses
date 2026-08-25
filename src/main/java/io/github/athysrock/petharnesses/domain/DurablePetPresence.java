package io.github.athysrock.petharnesses.domain;

/** Durable-world result used to make restore retries distinguish success from a UUID conflict. */
public enum DurablePetPresence { ABSENT, MATCHING_RESTORED, CONFLICT }
