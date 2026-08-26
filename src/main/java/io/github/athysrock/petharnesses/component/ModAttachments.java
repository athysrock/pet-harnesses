package io.github.athysrock.petharnesses.component;

import io.github.athysrock.petharnesses.PetHarnesses;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
    public static final AttachmentType<HarnessLink> PET_LINK = AttachmentRegistry.create(
            PetHarnesses.id("pet_link"), builder -> builder
                    .persistent(HarnessLink.CODEC)
                    .syncWith(HarnessLink.STREAM_CODEC, AttachmentSyncPredicate.all()));

    private ModAttachments() {}
    public static void register() {}
}
