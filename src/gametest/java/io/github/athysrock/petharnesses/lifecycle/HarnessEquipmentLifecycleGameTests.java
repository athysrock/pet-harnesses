package io.github.athysrock.petharnesses.lifecycle;

import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModAttachments;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.item.ModItems;
import io.github.athysrock.petharnesses.transaction.CaptureResult;
import io.github.athysrock.petharnesses.transaction.WolfCaptureTransaction;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Regression coverage for authoritative occupied-LEGS exit detection. */
public final class HarnessEquipmentLifecycleGameTests {
    @GameTest(maxTicks = 50)
    @SuppressWarnings("removal")
    public void flightAndCopiedEquipmentEventsDoNotRestoreEquippedHarness(GameTestHelper helper) {
        Fixture fixture = occupiedFixture(helper, "Airborne Authority Wolf");
        AtomicInteger restoreInvocations = new AtomicInteger();
        Runnable stopObserving = HarnessEquipmentLifecycle.observeRestoresForTest(
                player -> {
                    if (player == fixture.player()) restoreInvocations.incrementAndGet();
                });

        // Fabric supplies a copied previous stack. A transient callback projection must not override
        // the authoritative fact that the exact occupied harness still occupies LEGS.
        ServerEntityEvents.EQUIPMENT_CHANGE.invoker().onChange(
                fixture.player(), EquipmentSlot.LEGS, fixture.harness().copy(), ItemStack.EMPTY);

        for (int tick = 2; tick <= 34; tick++) {
            int currentTick = tick;
            helper.runAtTickTime(tick, () -> {
                fixture.player().getAbilities().flying = (currentTick & 1) == 0;
                fixture.player().startFallFlying();
                fixture.player().setDeltaMovement(
                        new Vec3(1.25 + currentTick * 0.01, (currentTick % 3 - 1) * 0.2, -0.7));
                fixture.player().snapTo(
                        helper.absoluteVec(new Vec3(4.5 + currentTick * 17.0, 90.0 + currentTick, 4.5)),
                        currentTick * 11.0F,
                        -20.0F + currentTick);
                assertStillOccupied(helper, fixture, restoreInvocations);
            });
        }
        helper.runAtTickTime(36, () -> {
            try {
                assertStillOccupied(helper, fixture, restoreInvocations);
            } finally {
                stopObserving.run();
                removePlayer(helper, fixture.player());
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    @SuppressWarnings("removal")
    public void confirmedGroundUnequipRestoresExactlyOnce(GameTestHelper helper) {
        Fixture fixture = occupiedFixture(helper, "Ground Exit Wolf");
        AtomicInteger restoreInvocations = new AtomicInteger();
        Runnable stopObserving = HarnessEquipmentLifecycle.observeRestoresForTest(
                player -> {
                    if (player == fixture.player()) restoreInvocations.incrementAndGet();
                });

        fixture.player().snapTo(helper.absoluteVec(new Vec3(4.5, 2.0, 4.5)), 0.0F, 0.0F);
        ItemStack occupiedPrevious = fixture.harness().copy();
        fixture.player().getInventory().setItem(0, fixture.harness());
        fixture.player().setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        ServerEntityEvents.EQUIPMENT_CHANGE.invoker().onChange(
                fixture.player(), EquipmentSlot.LEGS, occupiedPrevious, ItemStack.EMPTY);

        helper.runAtTickTime(5, () -> {
            try {
                helper.assertTrue(restoreInvocations.get() == 1,
                        "confirmed LEGS exit must invoke restore exactly once, got " + restoreInvocations.get());
                helper.assertTrue(!fixture.harness().has(ModComponents.CAPTURED_PET),
                        "successful restore must clear captured payload");
                helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) instanceof Wolf,
                        "confirmed ground exit must restore exactly the captured UUID");
                helper.assertTrue(HarnessStackLocator.find(
                                helper.getLevel().getServer(), fixture.link()).isEmpty(),
                        "restored linked stack must no longer count as occupied authority");
            } finally {
                stopObserving.run();
                removePlayer(helper, fixture.player());
            }
            helper.succeed();
        });
    }

    private static void assertStillOccupied(
            GameTestHelper helper, Fixture fixture, AtomicInteger restoreInvocations) {
        helper.assertTrue(restoreInvocations.get() == 0,
                "flight/component callbacks must never invoke restore while occupied harness remains in LEGS");
        helper.assertTrue(fixture.player().getItemBySlot(EquipmentSlot.LEGS) == fixture.harness(),
                "the exact occupied stack must remain authoritative in LEGS");
        helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET),
                "equipped harness must remain occupied");
        helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) == null,
                "captured wolf UUID must remain absent while equipped");
        var occupied = HarnessStackLocator.find(helper.getLevel().getServer(), fixture.link());
        helper.assertTrue(occupied.size() == 1 && occupied.getFirst().stack() == fixture.harness(),
                "there must be exactly one occupied stack authority");
    }

    private static Fixture occupiedFixture(GameTestHelper helper, String name) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(4.5, 2.0, 4.5)), 0.0F, 0.0F);
        Wolf wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(4, 2, 4));
        wolf.tame(player);
        wolf.setAge(0);
        wolf.setCustomName(Component.literal(name));

        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        InteractionResult interaction = ModItems.PET_HARNESS.interactLivingEntity(
                harness, player, wolf, InteractionHand.MAIN_HAND);
        helper.assertTrue(interaction == InteractionResult.SUCCESS, "fixture must link harness");
        HarnessLink link = harness.get(ModComponents.HARNESS_LINK);
        helper.assertTrue(link != null && link.equals(wolf.getAttached(ModAttachments.PET_LINK)),
                "fixture must establish reciprocal identity");
        player.setItemSlot(EquipmentSlot.LEGS, harness);
        if (!harness.has(ModComponents.CAPTURED_PET)) {
            CaptureResult result = WolfCaptureTransaction.capture(player, harness);
            helper.assertTrue(result == CaptureResult.SUCCESS, "fixture capture must succeed, got " + result);
        }
        return new Fixture(player, harness, link);
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private record Fixture(ServerPlayer player, ItemStack harness, HarnessLink link) {}
}
