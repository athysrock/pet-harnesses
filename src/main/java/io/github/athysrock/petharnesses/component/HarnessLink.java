package io.github.athysrock.petharnesses.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Reciprocal identity; linkId makes copied/stale human halves unable to detach a replacement link. */
public record HarnessLink(int version, UUID linkId, UUID ownerId, UUID petId) {
    public static final int CURRENT_VERSION = 1;
    public static final Codec<HarnessLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("version").forGetter(HarnessLink::version),
            UUIDUtil.LENIENT_CODEC.fieldOf("link_id").forGetter(HarnessLink::linkId),
            UUIDUtil.LENIENT_CODEC.fieldOf("owner").forGetter(HarnessLink::ownerId),
            UUIDUtil.LENIENT_CODEC.fieldOf("pet").forGetter(HarnessLink::petId)
    ).apply(instance, HarnessLink::new));

    public HarnessLink {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("version");
        Objects.requireNonNull(linkId);
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(petId);
    }

    public HarnessLink(UUID ownerId, UUID petId) { this(CURRENT_VERSION, UUID.randomUUID(), ownerId, petId); }
}
