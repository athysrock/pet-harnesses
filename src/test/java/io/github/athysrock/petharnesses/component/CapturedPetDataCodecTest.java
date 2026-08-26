package io.github.athysrock.petharnesses.component;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.DataResult;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

final class CapturedPetDataCodecTest {
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void codecPreservesCompleteArbitraryEntityAndEquipmentPayload() {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:wolf");
        entity.putInt("Age", -1234);
        entity.putFloat("Health", 7.5f);
        entity.putString("variant", "minecraft:ashen");
        CompoundTag equipment = new CompoundTag();
        equipment.putString("body", "minecraft:wolf_armor");
        equipment.putString("modded_slot", "example:charm");
        entity.put("equipment", equipment);
        CompoundTag arbitraryModData = new CompoundTag();
        arbitraryModData.putLong("energy", 987654321L);
        entity.put("example:arbitrary", arbitraryModData);
        CapturedPetData original = new CapturedPetData(1, PET, OWNER, entity);

        DataResult<net.minecraft.nbt.Tag> encoded = CapturedPetData.CODEC.encodeStart(NbtOps.INSTANCE, original);
        var decoded = CapturedPetData.CODEC.parse(NbtOps.INSTANCE, encoded.getOrThrow()).getOrThrow();

        assertEquals(1, decoded.version());
        assertEquals(PET, decoded.petId());
        assertEquals(OWNER, decoded.ownerId());
        assertEquals(entity, decoded.entityData());
    }

    @Test
    void codecRejectsUnsupportedFutureVersion() {
        CompoundTag encoded = new CompoundTag();
        encoded.putInt("version", CapturedPetData.CURRENT_VERSION + 1);
        encoded.putIntArray("pet", uuidInts(PET));
        encoded.putIntArray("owner", uuidInts(OWNER));
        encoded.put("entity", new CompoundTag());
        assertTrue(CapturedPetData.CODEC.parse(NbtOps.INSTANCE, encoded).error().isPresent());
    }

    private static int[] uuidInts(UUID id) {
        return new int[] {(int) (id.getMostSignificantBits() >> 32), (int) id.getMostSignificantBits(),
                (int) (id.getLeastSignificantBits() >> 32), (int) id.getLeastSignificantBits()};
    }
}
