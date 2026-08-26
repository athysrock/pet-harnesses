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
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/** UUID-preserving restore whose occupied stack remains authoritative until verified insertion. */
public final class WolfRestoreTransaction {
    private static final Logger LOGGER = LogUtils.getLogger();

    private WolfRestoreTransaction() {}

    public static RestoreResult restore(ServerPlayer owner, ItemStack actualStack) {
        return restore(owner, actualStack, FaultInjector.NONE);
    }

    static RestoreResult restore(
            ServerPlayer owner, ItemStack actualStack, FaultInjector faults) {
        java.util.Objects.requireNonNull(faults, "faults");
        MinecraftServer server = owner.level().getServer();
        requireServerThread(server);
        HarnessLink link = actualStack.get(ModComponents.HARNESS_LINK);
        CapturedPetData captured = actualStack.get(ModComponents.CAPTURED_PET);
        PetRenderData render = actualStack.get(ModComponents.PET_RENDER_DATA);
        if (!actualStack.is(ModItems.PET_HARNESS) || captured == null) {
            return RestoreResult.NOT_OCCUPIED;
        }
        if (link == null
                || render == null
                || !link.ownerId().equals(owner.getUUID())
                || !captured.petId().equals(link.petId())
                || !captured.ownerId().equals(link.ownerId())
                || !render.petId().equals(link.petId())
                || !EntityNbtValidator.isCanonicalAdultWolf(
                        captured.entityData(), captured.petId(), captured.ownerId())) {
            return RestoreResult.INVALID_PAYLOAD;
        }
        if (HarnessStackLocator.findOccupiedByPet(server, link.petId()).stream()
                .anyMatch(location -> location.stack() != actualStack)) {
            return RestoreResult.DUPLICATE_UUID;
        }

        Entity existing = WolfCaptureTransaction.findEntity(server, link.petId());
        ExistingPet presence = assessExisting(link, existing);
        if (presence == ExistingPet.CONFLICT) return RestoreResult.DUPLICATE_UUID;
        if (presence == ExistingPet.MATCHING) {
            return clearAfterVerifiedInsertion(
                    owner, actualStack, link, captured, render, (Wolf) existing, faults);
        }

        ServerLevel level = owner.level();
        Vec3 safe = SafeWolfPlacement.find(level, owner.blockPosition()).orElse(null);
        if (safe == null) return RestoreResult.NO_SAFE_POSITION;

        // Repeat the UUID preflight after the placement search and before reconstruction/insertion.
        if (WolfCaptureTransaction.findEntity(server, link.petId()) != null) {
            return RestoreResult.DUPLICATE_UUID;
        }
        Wolf loaded = load(level, captured);
        if (loaded == null
                || !loaded.getUUID().equals(link.petId())
                || loaded.isBaby()
                || !loaded.isAlive()
                || !loaded.isTame()
                || !loaded.isOwnedBy(owner)
                || loaded.isPassenger()
                || loaded.isVehicle()
                || !link.equals(loaded.getAttached(ModAttachments.PET_LINK))) {
            if (loaded != null) loaded.discard();
            return RestoreResult.RECONSTRUCTION_FAILED;
        }

        loaded.snapTo(safe, owner.getYRot(), 0.0F);
        loaded.setDeltaMovement(Vec3.ZERO);
        if (!SafeWolfPlacement.validatesLoadedWolf(level, loaded)) {
            loaded.discard();
            return RestoreResult.NO_SAFE_POSITION;
        }
        boolean inserted = level.addFreshEntity(loaded);
        if (!inserted || level.getEntity(link.petId()) != loaded) {
            if (level.getEntity(link.petId()) == null) loaded.discard();
            return RestoreResult.INSERTION_FAILED;
        }
        if (!reached(faults, RestoreBoundary.ENTITY_INSERTED, link)) {
            if (level.getEntity(link.petId()) == loaded) loaded.discard();
            return RestoreResult.INSERTION_FAILED;
        }
        return clearAfterVerifiedInsertion(
                owner, actualStack, link, captured, render, loaded, faults);
    }

    private static RestoreResult clearAfterVerifiedInsertion(
            ServerPlayer owner,
            ItemStack actualStack,
            HarnessLink link,
            CapturedPetData captured,
            PetRenderData render,
            Wolf inserted,
            FaultInjector faults) {
        if (!(inserted.level() instanceof ServerLevel level)
                || level.getEntity(link.petId()) != inserted
                || assessExisting(link, inserted) != ExistingPet.MATCHING) {
            return RestoreResult.INSERTION_FAILED;
        }
        if (!matchesPayload(actualStack, link, captured, render)) {
            return RestoreResult.CLEAR_RECONCILIATION_REQUIRED;
        }
        List<HarnessStackLocator.LocatedHarness> exactLocations =
                HarnessStackLocator.find(owner.level().getServer(), link);
        if (exactLocations.size() != 1 || exactLocations.getFirst().stack() != actualStack) {
            return RestoreResult.CLEAR_RECONCILIATION_REQUIRED;
        }

        actualStack.remove(ModComponents.CAPTURED_PET);
        actualStack.remove(ModComponents.PET_RENDER_DATA);
        if (actualStack.has(ModComponents.CAPTURED_PET)
                || actualStack.has(ModComponents.PET_RENDER_DATA)
                || !reached(faults, RestoreBoundary.PAYLOAD_CLEARED, link)) {
            restorePayload(actualStack, captured, render);
            return RestoreResult.CLEAR_RECONCILIATION_REQUIRED;
        }
        return RestoreResult.SUCCESS;
    }

    private static boolean matchesPayload(
            ItemStack stack,
            HarnessLink expectedLink,
            CapturedPetData expectedCaptured,
            PetRenderData expectedRender) {
        PetRenderData actualRender = stack.get(ModComponents.PET_RENDER_DATA);
        return stack.is(ModItems.PET_HARNESS)
                && expectedLink.equals(stack.get(ModComponents.HARNESS_LINK))
                && expectedCaptured.equals(stack.get(ModComponents.CAPTURED_PET))
                && expectedRender.matches(actualRender);
    }

    private static Wolf load(ServerLevel level, CapturedPetData captured) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
            ValueInput input = TagValueInput.create(
                    reporter, level.registryAccess(), captured.entityData());
            Entity loaded = EntityType.loadEntityRecursive(
                    input, level, EntitySpawnReason.LOAD, entity -> entity);
            return loaded instanceof Wolf wolf ? wolf : null;
        } catch (RuntimeException exception) {
            LOGGER.error("Could not reconstruct captured wolf {}", captured.petId(), exception);
            return null;
        }
    }

    private static void restorePayload(
            ItemStack stack, CapturedPetData captured, PetRenderData render) {
        stack.set(ModComponents.CAPTURED_PET, captured);
        stack.set(ModComponents.PET_RENDER_DATA, render);
    }

    private static boolean reached(
            FaultInjector faults, RestoreBoundary boundary, HarnessLink link) {
        try {
            faults.reached(boundary);
            return true;
        } catch (IOException exception) {
            LOGGER.error("Injected/observed restore failure at {} for {}",
                    boundary, link.petId(), exception);
            return false;
        }
    }

    private static ExistingPet assessExisting(HarnessLink expected, Entity entity) {
        if (entity == null) return ExistingPet.ABSENT;
        if (!(entity instanceof Wolf wolf) || wolf.getOwnerReference() == null) {
            return ExistingPet.CONFLICT;
        }
        return wolf.isAlive()
                && expected.petId().equals(wolf.getUUID())
                && expected.ownerId().equals(wolf.getOwnerReference().getUUID())
                && expected.equals(wolf.getAttached(ModAttachments.PET_LINK))
                ? ExistingPet.MATCHING
                : ExistingPet.CONFLICT;
    }

    private static void requireServerThread(MinecraftServer server) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("wolf restore must run on the server thread");
        }
    }

    enum RestoreBoundary {
        ENTITY_INSERTED,
        PAYLOAD_CLEARED
    }

    private enum ExistingPet { ABSENT, MATCHING, CONFLICT }

    @FunctionalInterface
    interface FaultInjector {
        FaultInjector NONE = boundary -> {};

        void reached(RestoreBoundary boundary) throws IOException;
    }
}
