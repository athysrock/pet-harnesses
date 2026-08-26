package io.github.athysrock.petharnesses.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/** Small, client-safe projection of an occupied adult wolf. Full entity NBT never enters this record. */
public record PetRenderData(
        int schema,
        UUID petId,
        Identifier texture,
        Optional<DyeColor> collarColor,
        ItemStack bodyArmor) {
    public static final int CURRENT_SCHEMA = 1;
    private static final Codec<Integer> SCHEMA_CODEC = Codec.INT.validate(schema -> schema == CURRENT_SCHEMA
            ? DataResult.success(schema)
            : DataResult.error(() -> "unsupported pet render schema: " + schema));
    public static final Codec<PetRenderData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SCHEMA_CODEC.fieldOf("schema").forGetter(PetRenderData::schema),
            UUIDUtil.LENIENT_CODEC.fieldOf("pet").forGetter(PetRenderData::petId),
            Identifier.CODEC.fieldOf("texture").forGetter(PetRenderData::texture),
            DyeColor.CODEC.optionalFieldOf("collar").forGetter(PetRenderData::collarColor),
            ItemStack.OPTIONAL_CODEC.fieldOf("body_armor").forGetter(PetRenderData::bodyArmor)
    ).apply(instance, PetRenderData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PetRenderData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetRenderData::schema,
            UUIDUtil.STREAM_CODEC, PetRenderData::petId,
            Identifier.STREAM_CODEC, PetRenderData::texture,
            ByteBufCodecs.optional(DyeColor.STREAM_CODEC), PetRenderData::collarColor,
            ItemStack.OPTIONAL_STREAM_CODEC, PetRenderData::bodyArmor,
            PetRenderData::new);

    public PetRenderData {
        if (schema != CURRENT_SCHEMA) throw new IllegalArgumentException("schema");
        Objects.requireNonNull(petId);
        Objects.requireNonNull(texture);
        collarColor = Objects.requireNonNull(collarColor);
        bodyArmor = Objects.requireNonNull(bodyArmor).copy();
    }

    @Override
    public ItemStack bodyArmor() {
        return bodyArmor.copy();
    }

    public boolean matches(PetRenderData other) {
        return other != null
                && schema == other.schema
                && petId.equals(other.petId)
                && texture.equals(other.texture)
                && collarColor.equals(other.collarColor)
                && ItemStack.matches(bodyArmor, other.bodyArmor);
    }
}
