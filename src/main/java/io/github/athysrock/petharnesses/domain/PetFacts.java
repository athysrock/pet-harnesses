package io.github.athysrock.petharnesses.domain;

import java.util.UUID;

public record PetFacts(Species species, boolean tamed, UUID ownerId, boolean alreadyLinked) {}
