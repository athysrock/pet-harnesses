package io.github.athysrock.petharnesses.component;

import io.github.athysrock.petharnesses.PetHarnesses;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModComponents {
    public static final DataComponentType<HarnessLink> HARNESS_LINK = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, PetHarnesses.id("harness_link"),
            DataComponentType.<HarnessLink>builder().persistent(HarnessLink.CODEC).cacheEncoding().build());
    public static final DataComponentType<CapturedPetData> CAPTURED_PET = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, PetHarnesses.id("captured_pet"),
            DataComponentType.<CapturedPetData>builder()
                    .persistent(CapturedPetData.CODEC)
                    .cacheEncoding()
                    .build());
    public static final DataComponentType<PetRenderData> PET_RENDER_DATA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, PetHarnesses.id("pet_render_data"),
            DataComponentType.<PetRenderData>builder()
                    .persistent(PetRenderData.CODEC)
                    .networkSynchronized(PetRenderData.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    private ModComponents() {}
    public static void register() {}
}
