package io.github.athysrock.petharnesses.item;

import io.github.athysrock.petharnesses.PetHarnesses;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final ResourceKey<Item> PET_HARNESS_KEY = ResourceKey.create(
            BuiltInRegistries.ITEM.key(), PetHarnesses.id("pet_harness"));
    public static final HarnessItem PET_HARNESS = Registry.register(
            BuiltInRegistries.ITEM, PET_HARNESS_KEY,
            new HarnessItem(new Item.Properties().setId(PET_HARNESS_KEY)
                    .stacksTo(HarnessDefinition.MAX_STACK_SIZE).equippable(EquipmentSlot.LEGS)));

    private ModItems() {}
    public static void register() {}
}
