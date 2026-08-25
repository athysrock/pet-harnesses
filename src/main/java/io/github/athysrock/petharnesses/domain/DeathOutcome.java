package io.github.athysrock.petharnesses.domain;

public record DeathOutcome(HarnessState state, DeathDisposition disposition, TransitionError error) {}
