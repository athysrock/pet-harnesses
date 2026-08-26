package io.github.athysrock.petharnesses.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

final class PetRenderDataCodecTest {
    @Test
    void persistentCodecRoundTripsOnlyCompactAdultWolfRenderFields() {
        PetRenderData original = new PetRenderData(
                PetRenderData.CURRENT_SCHEMA,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Identifier.withDefaultNamespace("textures/entity/wolf/wolf_tame.png"),
                Optional.of(DyeColor.BLUE),
                ItemStack.EMPTY);

        DataResult<net.minecraft.nbt.Tag> encoded = PetRenderData.CODEC.encodeStart(NbtOps.INSTANCE, original);
        PetRenderData decoded = PetRenderData.CODEC.parse(NbtOps.INSTANCE, encoded.getOrThrow()).getOrThrow();

        assertEquals(original.schema(), decoded.schema());
        assertEquals(original.petId(), decoded.petId());
        assertEquals(original.texture(), decoded.texture());
        assertEquals(original.collarColor(), decoded.collarColor());
        assertEquals(ItemStack.EMPTY, decoded.bodyArmor());
    }
}
