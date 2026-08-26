package io.github.athysrock.petharnesses.client;

import io.github.athysrock.petharnesses.component.PetRenderData;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

public final class ClientRenderDataKeys {
    public static final RenderStateDataKey<Boolean> HARNESS_EQUIPPED = RenderStateDataKey.create(
            () -> "pet_harnesses:lederhosen_equipped");
    public static final RenderStateDataKey<PetRenderData> PET = RenderStateDataKey.create(
            () -> "pet_harnesses:carried_wolf");
    public static final RenderStateDataKey<CarriedWolfMotionSnapshot> MOTION = RenderStateDataKey.create(
            () -> "pet_harnesses:carried_wolf_motion");

    private ClientRenderDataKeys() {}
}
