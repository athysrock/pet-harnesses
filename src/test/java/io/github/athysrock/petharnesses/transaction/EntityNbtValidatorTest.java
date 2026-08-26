package io.github.athysrock.petharnesses.transaction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class EntityNbtValidatorTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void acceptsOnlyExactWolfUuidAndOwnerAndPreservesOpaqueFields() {
        CompoundTag tag = canonical();
        CompoundTag equipment = new CompoundTag();
        equipment.putString("body", "minecraft:wolf_armor");
        tag.put("equipment", equipment);
        tag.putString("example:opaque", "preserved");

        assertTrue(EntityNbtValidator.isCanonicalAdultWolf(tag, PET, OWNER));
        assertEquals("preserved", tag.getStringOr("example:opaque", ""));
        assertEquals(equipment, tag.getCompound("equipment").orElseThrow());
    }

    @Test
    void rejectsWrongTypeUuidOwnerAndBabyPayload() {
        CompoundTag tag = canonical();
        tag.putString("id", "minecraft:cat");
        assertFalse(EntityNbtValidator.isCanonicalAdultWolf(tag, PET, OWNER));
        tag = canonical();
        tag.putIntArray("UUID", UUIDUtil.uuidToIntArray(UUID.randomUUID()));
        assertFalse(EntityNbtValidator.isCanonicalAdultWolf(tag, PET, OWNER));
        tag = canonical();
        tag.putIntArray("Owner", UUIDUtil.uuidToIntArray(UUID.randomUUID()));
        assertFalse(EntityNbtValidator.isCanonicalAdultWolf(tag, PET, OWNER));
        tag = canonical();
        tag.putInt("Age", -20);
        assertFalse(EntityNbtValidator.isCanonicalAdultWolf(tag, PET, OWNER));
        tag = canonical();
        tag.remove("Age");
        assertFalse(EntityNbtValidator.isCanonicalAdultWolf(tag, PET, OWNER));
    }

    private static CompoundTag canonical() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:wolf");
        tag.putIntArray("UUID", UUIDUtil.uuidToIntArray(PET));
        tag.putIntArray("Owner", UUIDUtil.uuidToIntArray(OWNER));
        tag.putInt("Age", 0);
        return tag;
    }
}
