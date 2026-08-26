package io.github.athysrock.petharnesses.client;

import io.github.athysrock.petharnesses.physics.MotionInput;

public record CarriedWolfMotionSnapshot(int playerEntityId, long gameTick, MotionInput input) {}
