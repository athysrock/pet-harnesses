package io.github.athysrock.petharnesses.transaction;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

/** Strict identity/type boundary for complete vanilla entity serialization. */
public final class EntityNbtValidator {
    private EntityNbtValidator() {}

    public static boolean isCanonicalAdultWolf(CompoundTag tag, UUID expectedPet, UUID expectedOwner) {
        if (!"minecraft:wolf".equals(tag.getStringOr("id", ""))) return false;
        if (!expectedPet.equals(readUuid(tag, "UUID"))) return false;
        if (!expectedOwner.equals(readUuid(tag, "Owner"))) return false;
        return tag.getInt("Age").orElse(Integer.MIN_VALUE) >= 0;
    }

    private static UUID readUuid(CompoundTag tag, String key) {
        int[] values = tag.getIntArray(key).orElse(null);
        return values != null && values.length == 4 ? UUIDUtil.uuidFromIntArray(values) : null;
    }
}
