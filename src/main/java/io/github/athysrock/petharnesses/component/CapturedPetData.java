package io.github.athysrock.petharnesses.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

/** Complete, versioned entity NBT retained only while an occupied harness is equipped. */
public record CapturedPetData(
        int version, UUID petId, UUID ownerId, CompoundTag entityData) {
    public static final int CURRENT_VERSION = 1;
    private static final Codec<Integer> VERSION_CODEC = Codec.INT.validate(version -> version == CURRENT_VERSION
            ? com.mojang.serialization.DataResult.success(version)
            : com.mojang.serialization.DataResult.error(() -> "unsupported captured pet version: " + version));
    public static final Codec<CapturedPetData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VERSION_CODEC.fieldOf("version").forGetter(CapturedPetData::version),
            UUIDUtil.LENIENT_CODEC.fieldOf("pet").forGetter(CapturedPetData::petId),
            UUIDUtil.LENIENT_CODEC.fieldOf("owner").forGetter(CapturedPetData::ownerId),
            CompoundTag.CODEC.fieldOf("entity").forGetter(CapturedPetData::entityData)
    ).apply(instance, CapturedPetData::new));

    public CapturedPetData {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("version");
        Objects.requireNonNull(petId);
        Objects.requireNonNull(ownerId);
        entityData = Objects.requireNonNull(entityData).copy();
    }

    @Override public CompoundTag entityData() { return entityData.copy(); }
}
