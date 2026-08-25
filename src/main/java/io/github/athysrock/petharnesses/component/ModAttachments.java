package io.github.athysrock.petharnesses.component;

import io.github.athysrock.petharnesses.PetHarnesses;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
    public static final AttachmentType<HarnessLink> PET_LINK = AttachmentRegistry.createPersistent(
            PetHarnesses.id("pet_link"), HarnessLink.CODEC);

    private ModAttachments() {}
    public static void register() {}
}
