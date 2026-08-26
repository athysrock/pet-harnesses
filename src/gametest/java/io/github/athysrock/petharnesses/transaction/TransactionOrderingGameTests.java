package io.github.athysrock.petharnesses.transaction;

import io.github.athysrock.petharnesses.component.CapturedPetData;
import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModAttachments;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.component.PetRenderData;
import io.github.athysrock.petharnesses.item.ModItems;
import io.github.athysrock.petharnesses.lifecycle.HarnessStackLocator;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Ordinary in-memory transaction ordering against real 26.2 entities and stacks. */
public final class TransactionOrderingGameTests {
    @GameTest(maxTicks = 40)
    @SuppressWarnings("removal")
    public void captureValidatesActualOccupiedLegsStackBeforeDiscard(GameTestHelper helper) {
        Fixture fixture = linkedFixture(helper, "Capture commit ordering");
        try {
            CaptureResult result = WolfCaptureTransaction.capture(
                    fixture.player(), fixture.harness(), boundary -> {
                        if (boundary == WolfCaptureTransaction.CaptureBoundary.PAYLOAD_MUTATED) {
                            fixture.player().setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                        }
                    });

            helper.assertTrue(result == CaptureResult.COMMIT_FAILED,
                    "replacing the occupied LEGS object must fail commit validation");
            helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) == fixture.wolf(),
                    "commit validation failure must leave the exact live wolf in the level");
            helper.assertTrue(fixture.wolf().isAlive() && !fixture.wolf().isRemoved(),
                    "capture must not discard before the actual occupied stack commits");
        } finally {
            cleanup(helper, fixture);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    @SuppressWarnings("removal")
    public void captureFailureBeforeDiscardRetainsWolfAndFullPayload(GameTestHelper helper) {
        Fixture fixture = linkedFixture(helper, "Capture discard ordering");
        try {
            CaptureResult result = WolfCaptureTransaction.capture(
                    fixture.player(), fixture.harness(), boundary -> {
                        if (boundary == WolfCaptureTransaction.CaptureBoundary.WOLF_DISCARD) {
                            throw new IOException("ordinary injected failure before discard");
                        }
                    });

            helper.assertTrue(result != CaptureResult.SUCCESS,
                    "failure before discard must interrupt capture");
            helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET),
                    "the committed stack must retain exact full NBT when discard does not happen");
            helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) == fixture.wolf(),
                    "failure before discard must retain the original live wolf");
        } finally {
            cleanup(helper, fixture);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    @SuppressWarnings("removal")
    public void restoreUsesExactOccupiedPayloadWithoutExternalAuthority(GameTestHelper helper) {
        Fixture fixture = occupiedFromExactNbt(helper, "Payload-only restore");
        try {
            RestoreResult result = WolfRestoreTransaction.restore(
                    fixture.player(), fixture.harness());

            helper.assertTrue(result == RestoreResult.SUCCESS,
                    "a valid occupied stack must restore from its own exact full NBT");
            helper.assertTrue(!fixture.harness().has(ModComponents.CAPTURED_PET),
                    "payload clears only after successful verified insertion");
            helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) instanceof Wolf,
                    "restore must insert the exact captured UUID");
        } finally {
            cleanup(helper, fixture);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    @SuppressWarnings("removal")
    public void restoreInsertionAndClearFailuresRetainPayload(GameTestHelper helper) {
        Fixture insertionFailure = occupiedFixture(helper, "Restore insertion failure");
        try {
            RestoreResult result = WolfRestoreTransaction.restore(
                    insertionFailure.player(), insertionFailure.harness(), boundary -> {
                        if (boundary == WolfRestoreTransaction.RestoreBoundary.ENTITY_INSERTED) {
                            throw new IOException("ordinary injected failure after insertion");
                        }
                    });
            assertPayloadRetained(helper, insertionFailure,
                    "post-insertion failure must retain the complete payload");
            helper.assertTrue(result != RestoreResult.SUCCESS,
                    "post-insertion failure must not report success");
        } finally {
            cleanup(helper, insertionFailure);
        }

        Fixture clearFailure = occupiedFixture(helper, "Restore clear failure");
        try {
            RestoreResult first = WolfRestoreTransaction.restore(
                    clearFailure.player(), clearFailure.harness(), boundary -> {
                        if (boundary == WolfRestoreTransaction.RestoreBoundary.PAYLOAD_CLEARED) {
                            throw new IOException("ordinary injected clear failure");
                        }
                    });
            helper.assertTrue(first == RestoreResult.CLEAR_RECONCILIATION_REQUIRED,
                    "clear fault must require reconciliation, got " + first);
            assertPayloadRetained(helper, clearFailure,
                    "clear failure must restore the complete payload");
            Wolf inserted = (Wolf) helper.getLevel().getEntity(clearFailure.link().petId());
            helper.assertTrue(inserted != null,
                    "clear reconciliation must retain the exact inserted wolf");

            RestoreResult retry = WolfRestoreTransaction.restore(
                    clearFailure.player(), clearFailure.harness());
            helper.assertTrue(retry == RestoreResult.SUCCESS,
                    "retry without a fault must reconcile and succeed, got " + retry);
            helper.assertTrue(helper.getLevel().getEntity(clearFailure.link().petId()) == inserted,
                    "retry must recognize the same live reciprocal wolf, not insert a duplicate");
            helper.assertTrue(!clearFailure.harness().has(ModComponents.CAPTURED_PET)
                            && !clearFailure.harness().has(ModComponents.PET_RENDER_DATA),
                    "successful retry must clear retained payload and render authority");
            helper.assertTrue(inserted.getUUID().equals(clearFailure.link().petId())
                            && inserted.isOwnedBy(clearFailure.player())
                            && clearFailure.link().equals(inserted.getAttached(ModAttachments.PET_LINK))
                            && clearFailure.link().equals(
                                    clearFailure.harness().get(ModComponents.HARNESS_LINK)),
                    "retry must preserve UUID, owner, and reciprocal link identity");
        } finally {
            cleanup(helper, clearFailure);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    @SuppressWarnings("removal")
    public void locatorRejectsIndependentOccupiedIdentityMismatches(GameTestHelper helper) {
        Fixture fixture = occupiedFixture(helper, "Occupied identity matching");
        ItemStack harness = fixture.harness();
        HarnessLink canonicalLink = fixture.link();
        CapturedPetData canonicalCaptured = harness.get(ModComponents.CAPTURED_PET);
        PetRenderData canonicalRender = harness.get(ModComponents.PET_RENDER_DATA);
        try {
            var canonicalLocations = HarnessStackLocator.find(
                    helper.getLevel().getServer(), canonicalLink);
            helper.assertTrue(HarnessStackLocator.isCanonicalOccupied(harness)
                            && canonicalLocations.size() == 1
                            && canonicalLocations.getFirst().stack() == harness,
                    "canonical occupied authority must be discoverable as the actual stack");

            HarnessLink mismatchedLink = new HarnessLink(
                    HarnessLink.CURRENT_VERSION, UUID.randomUUID(),
                    canonicalLink.ownerId(), canonicalLink.petId());
            harness.set(ModComponents.HARNESS_LINK, mismatchedLink);
            assertMismatchRejected(helper, fixture, canonicalLink,
                    "HarnessLink identity mismatch");
            harness.set(ModComponents.HARNESS_LINK, canonicalLink);

            harness.set(ModComponents.CAPTURED_PET, new CapturedPetData(
                    CapturedPetData.CURRENT_VERSION, UUID.randomUUID(),
                    canonicalLink.ownerId(), canonicalCaptured.entityData()));
            assertMismatchRejected(helper, fixture, canonicalLink,
                    "captured pet identity mismatch");
            harness.set(ModComponents.CAPTURED_PET, canonicalCaptured);

            harness.set(ModComponents.CAPTURED_PET, new CapturedPetData(
                    CapturedPetData.CURRENT_VERSION, canonicalLink.petId(),
                    UUID.randomUUID(), canonicalCaptured.entityData()));
            assertMismatchRejected(helper, fixture, canonicalLink,
                    "captured owner identity mismatch");
            harness.set(ModComponents.CAPTURED_PET, canonicalCaptured);

            harness.set(ModComponents.PET_RENDER_DATA, new PetRenderData(
                    PetRenderData.CURRENT_SCHEMA, UUID.randomUUID(), canonicalRender.texture(),
                    canonicalRender.collarColor(), canonicalRender.bodyArmor()));
            assertMismatchRejected(helper, fixture, canonicalLink,
                    "render pet identity mismatch");
            harness.set(ModComponents.PET_RENDER_DATA, canonicalRender);
        } finally {
            cleanup(helper, fixture);
        }
        helper.succeed();
    }

    private static void assertMismatchRejected(
            GameTestHelper helper, Fixture fixture, HarnessLink canonicalLink, String mismatch) {
        helper.assertTrue(!HarnessStackLocator.isCanonicalOccupied(fixture.harness())
                        || !HarnessStackLocator.find(
                                helper.getLevel().getServer(), canonicalLink).stream()
                                .anyMatch(location -> location.stack() == fixture.harness()),
                mismatch + " must not authorize canonical lookup");
        RestoreResult result = WolfRestoreTransaction.restore(
                fixture.player(), fixture.harness());
        helper.assertTrue(result != RestoreResult.SUCCESS,
                mismatch + " must not authorize restoration/clear, got " + result);
        helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET)
                        && fixture.harness().has(ModComponents.PET_RENDER_DATA),
                mismatch + " must retain payload and render authority");
        helper.assertTrue(helper.getLevel().getEntity(canonicalLink.petId()) == null,
                mismatch + " must not materialize the captured wolf");
    }

    private static Fixture occupiedFixture(GameTestHelper helper, String name) {
        Fixture fixture = linkedFixture(helper, name);
        helper.assertTrue(WolfCaptureTransaction.capture(fixture.player(), fixture.harness())
                        == CaptureResult.SUCCESS,
                "fixture capture must commit");
        return fixture;
    }

    private static Fixture occupiedFromExactNbt(GameTestHelper helper, String name) {
        Fixture fixture = linkedFixture(helper, name);
        HarnessLink occupiedLink = fixture.link();
        CompoundTag entityData;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(
                fixture.wolf().problemPath(), LoggerFactory.getLogger(TransactionOrderingGameTests.class))) {
            TagValueOutput output = TagValueOutput.createWithContext(
                    reporter, fixture.wolf().registryAccess());
            helper.assertTrue(fixture.wolf().save(output), "fixture wolf must serialize exactly");
            entityData = output.buildResult();
        }
        fixture.harness().set(ModComponents.CAPTURED_PET, new CapturedPetData(
                CapturedPetData.CURRENT_VERSION,
                occupiedLink.petId(),
                occupiedLink.ownerId(),
                entityData));
        fixture.harness().set(ModComponents.PET_RENDER_DATA, new PetRenderData(
                PetRenderData.CURRENT_SCHEMA,
                occupiedLink.petId(),
                fixture.wolf().getTexture(),
                Optional.of(fixture.wolf().getCollarColor()),
                fixture.wolf().getBodyArmorItem()));
        fixture.wolf().discard();
        return new Fixture(
                fixture.player(), fixture.wolf(), fixture.harness(), occupiedLink);
    }

    private static Fixture linkedFixture(GameTestHelper helper, String name) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(4.5, 2.0, 4.5)), 0.0F, 0.0F);
        Wolf wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(4, 2, 4));
        wolf.tame(player);
        wolf.setAge(0);
        wolf.setCustomName(Component.literal(name));
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        InteractionResult interaction = ModItems.PET_HARNESS.interactLivingEntity(
                harness, player, wolf, InteractionHand.MAIN_HAND);
        helper.assertTrue(interaction == InteractionResult.SUCCESS, "fixture must link");
        HarnessLink link = harness.get(ModComponents.HARNESS_LINK);
        helper.assertTrue(link != null && link.equals(wolf.getAttached(ModAttachments.PET_LINK)),
                "fixture reciprocal link must match");
        player.setItemSlot(EquipmentSlot.LEGS, harness);
        return new Fixture(player, wolf, harness, link);
    }

    private static void assertPayloadRetained(
            GameTestHelper helper, Fixture fixture, String message) {
        helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET), message);
        helper.assertTrue(fixture.harness().has(ModComponents.PET_RENDER_DATA),
                message + " (render projection)");
    }

    private static void cleanup(GameTestHelper helper, Fixture fixture) {
        Wolf live = (Wolf) helper.getLevel().getEntity(fixture.link().petId());
        if (live != null) live.discard();
        helper.getLevel().getServer().getPlayerList().remove(fixture.player());
    }

    private record Fixture(
            ServerPlayer player, Wolf wolf, ItemStack harness, HarnessLink link) {}
}
