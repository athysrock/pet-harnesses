package io.github.athysrock.petharnesses.network;

import io.github.athysrock.petharnesses.PetHarnesses;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Removes the server-only payload component, including removal markers, from item-stack packets. */
public final class CapturedPetNetworkSanitizer {
    private static final Identifier CAPTURED_PET_ID = PetHarnesses.id("captured_pet");

    private CapturedPetNetworkSanitizer() {}

    public static DataComponentPatch forNetwork(DataComponentPatch source) {
        return source.forget(type -> CAPTURED_PET_ID.equals(
                BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)));
    }

    public static DataComponentPatch forNetwork(
            DataComponentPatch source, DataComponentType<?> serverOnlyType) {
        return source.forget(type -> type == serverOnlyType);
    }
}
