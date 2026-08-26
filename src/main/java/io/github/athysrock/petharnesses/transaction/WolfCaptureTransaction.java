package io.github.athysrock.petharnesses.transaction;

import com.mojang.logging.LogUtils;
import io.github.athysrock.petharnesses.component.CapturedPetData;
import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModAttachments;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.component.PetRenderData;
import io.github.athysrock.petharnesses.item.ModItems;
import io.github.athysrock.petharnesses.lifecycle.HarnessStackLocator;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

/** Server transaction: commit exact wolf NBT to the equipped stack before discarding the wolf. */
public final class WolfCaptureTransaction {
    private static final Logger LOGGER = LogUtils.getLogger();

    private WolfCaptureTransaction() {}

    public static CaptureResult capture(ServerPlayer player, ItemStack stack) {
        return capture(player, stack, FaultInjector.NONE);
    }

    static CaptureResult capture(
            ServerPlayer player, ItemStack stack, FaultInjector faults) {
        java.util.Objects.requireNonNull(faults, "faults");
        MinecraftServer server = player.level().getServer();
        requireServerThread(server);
        if (!stack.is(ModItems.PET_HARNESS) || stack.has(ModComponents.CAPTURED_PET)) {
            return CaptureResult.NOT_LINKED;
        }
        HarnessLink link = stack.get(ModComponents.HARNESS_LINK);
        if (link == null
                || !link.ownerId().equals(player.getUUID())
                || player.getItemBySlot(EquipmentSlot.LEGS) != stack) {
            return CaptureResult.INVALID_LINK;
        }

        Entity candidate = player.level().getEntity(link.petId());
        if (!(candidate instanceof Wolf wolf)
                || !wolf.isAlive()
                || wolf.isBaby()
                || wolf.isLeashed()
                || wolf.isPassenger()
                || wolf.isVehicle()
                || !wolf.isTame()
                || !wolf.isOwnedBy(player)
                || !link.equals(wolf.getAttached(ModAttachments.PET_LINK))) {
            return CaptureResult.PET_UNAVAILABLE;
        }
        if (hasAnotherOccupiedRepresentation(server, stack, link)) {
            return CaptureResult.DUPLICATE_REPRESENTATION;
        }

        CompoundTag entityData;
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(wolf.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(
                    reporter, wolf.registryAccess());
            if (!wolf.save(output)) return CaptureResult.SERIALIZATION_FAILED;
            entityData = output.buildResult();
        } catch (RuntimeException exception) {
            LOGGER.error("Could not serialize linked wolf {} for harness {}",
                    link.petId(), link.linkId(), exception);
            return CaptureResult.SERIALIZATION_FAILED;
        }
        if (!EntityNbtValidator.isCanonicalAdultWolf(
                entityData, link.petId(), link.ownerId())) {
            return CaptureResult.SERIALIZATION_FAILED;
        }

        CapturedPetData captured = new CapturedPetData(
                CapturedPetData.CURRENT_VERSION,
                link.petId(),
                link.ownerId(),
                entityData);
        PetRenderData renderData = new PetRenderData(
                PetRenderData.CURRENT_SCHEMA,
                link.petId(),
                wolf.getTexture(),
                java.util.Optional.of(wolf.getCollarColor()),
                wolf.getBodyArmorItem());

        // Recheck the exact occupied object immediately before committing the authority payload.
        if (player.getItemBySlot(EquipmentSlot.LEGS) != stack) {
            return CaptureResult.COMMIT_FAILED;
        }
        stack.set(ModComponents.CAPTURED_PET, captured);
        stack.set(ModComponents.PET_RENDER_DATA, renderData);
        if (!reached(faults, CaptureBoundary.PAYLOAD_MUTATED, link)) {
            return CaptureResult.COMMIT_FAILED;
        }
        if (!committed(player, stack, link, captured, renderData)) {
            return CaptureResult.COMMIT_FAILED;
        }

        if (!reached(faults, CaptureBoundary.WOLF_DISCARD, link)) {
            return CaptureResult.REMOVAL_RECONCILIATION_REQUIRED;
        }
        wolf.discard();
        return wolf.isRemoved() && findEntity(server, link.petId()) == null
                ? CaptureResult.SUCCESS
                : CaptureResult.REMOVAL_RECONCILIATION_REQUIRED;
    }

    /** Completes the ordinary in-memory window where the payload committed before discard. */
    public static CaptureResult reconcileOccupied(ServerPlayer player, ItemStack stack) {
        MinecraftServer server = player.level().getServer();
        requireServerThread(server);
        HarnessLink link = stack.get(ModComponents.HARNESS_LINK);
        CapturedPetData captured = stack.get(ModComponents.CAPTURED_PET);
        PetRenderData render = stack.get(ModComponents.PET_RENDER_DATA);
        if (link == null || captured == null || render == null
                || !link.ownerId().equals(player.getUUID())
                || !link.petId().equals(captured.petId())
                || !link.ownerId().equals(captured.ownerId())
                || !link.petId().equals(render.petId())
                || !EntityNbtValidator.isCanonicalAdultWolf(
                        captured.entityData(), captured.petId(), captured.ownerId())) {
            return CaptureResult.INVALID_LINK;
        }
        if (HarnessStackLocator.findOccupiedByPet(server, link.petId()).stream()
                .anyMatch(location -> location.stack() != stack)) {
            return CaptureResult.DUPLICATE_REPRESENTATION;
        }

        Entity existing = findEntity(server, link.petId());
        if (existing == null) return CaptureResult.SUCCESS;
        if (!(existing instanceof Wolf wolf)
                || !wolf.isAlive()
                || wolf.isBaby()
                || !wolf.isOwnedBy(player)
                || !link.equals(wolf.getAttached(ModAttachments.PET_LINK))) {
            return CaptureResult.DUPLICATE_REPRESENTATION;
        }
        wolf.discard();
        return wolf.isRemoved() && findEntity(server, link.petId()) == null
                ? CaptureResult.SUCCESS
                : CaptureResult.REMOVAL_RECONCILIATION_REQUIRED;
    }

    private static boolean committed(
            ServerPlayer player,
            ItemStack stack,
            HarnessLink expectedLink,
            CapturedPetData expectedCaptured,
            PetRenderData expectedRender) {
        ItemStack actual = player.getItemBySlot(EquipmentSlot.LEGS);
        CapturedPetData captured = actual.get(ModComponents.CAPTURED_PET);
        PetRenderData render = actual.get(ModComponents.PET_RENDER_DATA);
        return actual == stack
                && expectedLink.equals(actual.get(ModComponents.HARNESS_LINK))
                && expectedCaptured.equals(captured)
                && expectedRender.matches(render);
    }

    private static boolean hasAnotherOccupiedRepresentation(
            MinecraftServer server, ItemStack current, HarnessLink link) {
        return HarnessStackLocator.findOccupiedByPet(server, link.petId()).stream()
                .anyMatch(location -> location.stack() != current);
    }

    public static Entity findEntity(MinecraftServer server, java.util.UUID id) {
        requireServerThread(server);
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null && !entity.isRemoved()) return entity;
        }
        return null;
    }

    private static void requireServerThread(MinecraftServer server) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("wolf capture must run on the server thread");
        }
    }

    private static boolean reached(
            FaultInjector faults, CaptureBoundary boundary, HarnessLink link) {
        try {
            faults.reached(boundary);
            return true;
        } catch (IOException exception) {
            LOGGER.error("Injected/observed capture failure at {} for {}",
                    boundary, link.petId(), exception);
            return false;
        }
    }

    enum CaptureBoundary {
        PAYLOAD_MUTATED,
        WOLF_DISCARD
    }

    @FunctionalInterface
    interface FaultInjector {
        FaultInjector NONE = boundary -> {};

        void reached(CaptureBoundary boundary) throws IOException;
    }
}
