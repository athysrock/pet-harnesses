package io.github.athysrock.petharnesses;

import io.github.athysrock.petharnesses.component.ModAttachments;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public final class PetHarnesses implements ModInitializer {
    public static final String MOD_ID = "pet_harnesses";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.register();
        ModAttachments.register();
        ModItems.register();
    }
}
