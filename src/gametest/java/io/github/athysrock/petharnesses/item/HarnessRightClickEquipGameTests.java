package io.github.athysrock.petharnesses.item;

import io.github.athysrock.petharnesses.component.CapturedPetData;
import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModAttachments;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.component.PetRenderData;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Gameplay-path coverage for harness-specific equip authority and duplication risks. */
public final class HarnessRightClickEquipGameTests {
    @GameTest(maxTicks = 20)
    @SuppressWarnings("removal")
    public void fullInventoryStillSwapsLeggingsDirectlyToHand(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
        ItemStack expectedLeggings = leggings.copy();
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        player.setItemSlot(EquipmentSlot.LEGS, leggings);
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);

        InteractionResult result = player.gameMode.useItem(
                player, helper.getLevel(), harness, InteractionHand.MAIN_HAND);

        helper.assertTrue(result instanceof InteractionResult.Success,
                "a full inventory must not prevent the one-item vanilla hand swap");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PET_HARNESS),
                "full inventory swap must equip the harness");
        helper.assertTrue(ItemStack.matches(player.getMainHandItem(), expectedLeggings),
                "full inventory swap must return leggings directly to hand, not drop or delete them");
        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    @SuppressWarnings("removal")
    public void rightClickLinkedHarnessCapturesOnceAfterEquip(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        Wolf wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(4, 2, 4));
        wolf.tame(player);
        wolf.setAge(0);
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        helper.assertTrue(ModItems.PET_HARNESS.interactLivingEntity(
                        harness, player, wolf, InteractionHand.MAIN_HAND) instanceof InteractionResult.Success,
                "fixture must create a real reciprocal link");
        HarnessLink link = harness.get(ModComponents.HARNESS_LINK);
        helper.assertTrue(link != null && link.equals(wolf.getAttached(ModAttachments.PET_LINK)),
                "fixture link identities must match");
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);

        InteractionResult result = player.gameMode.useItem(
                player, helper.getLevel(), harness, InteractionHand.MAIN_HAND);
        helper.assertTrue(result instanceof InteractionResult.Success,
                "linked harness must first equip through vanilla use");
        ServerEntityEvents.EQUIPMENT_CHANGE.invoker().onChange(
                player, EquipmentSlot.LEGS, ItemStack.EMPTY,
                player.getItemBySlot(EquipmentSlot.LEGS));

        helper.runAtTickTime(3, () -> {
            ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
            helper.assertTrue(legs.is(ModItems.PET_HARNESS)
                            && legs.has(ModComponents.CAPTURED_PET)
                            && legs.has(ModComponents.PET_RENDER_DATA),
                    "the single equipment transition must produce one occupied LEGS authority");
            helper.assertTrue(helper.getLevel().getEntity(link.petId()) == null,
                    "linked wolf must be captured exactly once after the harness enters LEGS");
            helper.assertTrue(countHarnesses(player) == 1,
                    "survival linked equip must leave one harness stack, got " + countHarnesses(player));
            removePlayer(helper, player);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    @SuppressWarnings("removal")
    public void repeatedUseCannotEquipTwice(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);

        InteractionResult first = player.gameMode.useItem(
                player, helper.getLevel(), harness, InteractionHand.MAIN_HAND);
        ItemStack emptyHand = player.getMainHandItem();
        InteractionResult second = player.gameMode.useItem(
                player, helper.getLevel(), emptyHand, InteractionHand.MAIN_HAND);

        helper.assertTrue(first instanceof InteractionResult.Success,
                "first use must equip");
        helper.assertTrue(!(second instanceof InteractionResult.Success),
                "the consumed hand stack must not produce a second equip");
        helper.assertTrue(countHarnesses(player) == 1,
                "repeated server use must leave one harness, got " + countHarnesses(player));
        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    @SuppressWarnings("removal")
    public void creativeWholeHarnessUsesVanillaCopySemanticsWithoutDoubleEquip(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);

        InteractionResult first = player.gameMode.useItem(
                player, helper.getLevel(), harness, InteractionHand.MAIN_HAND);
        InteractionResult second = player.gameMode.useItem(
                player, helper.getLevel(), harness, InteractionHand.MAIN_HAND);

        helper.assertTrue(first instanceof InteractionResult.Success,
                "creative equip must succeed with vanilla copy semantics");
        helper.assertTrue(second instanceof InteractionResult.Fail,
                "identical second creative equip must be rejected by vanilla Equippable");
        helper.assertTrue(player.getMainHandItem().is(ModItems.PET_HARNESS)
                        && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PET_HARNESS)
                        && countHarnesses(player) == 2,
                "creative whole equip must retain one hand stack and exactly one equipped copy");
        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    @SuppressWarnings("removal")
    public void spectatorUseDoesNotEquip(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);

        InteractionResult result = player.gameMode.useItem(
                player, helper.getLevel(), harness, InteractionHand.MAIN_HAND);

        helper.assertTrue(result instanceof InteractionResult.Pass,
                "vanilla server game mode must pass spectator item use");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                        && player.getMainHandItem() == harness,
                "spectator use must not mutate either slot");
        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    @SuppressWarnings("removal")
    public void ownerShiftUseClearsOnlyAStaleItemLink(GameTestHelper helper) {
        ServerPlayer owner = survivalPlayer(helper);
        ServerPlayer foreignPlayer = survivalPlayer(helper);

        ItemStack stale = linkedHarness(owner.getUUID(), UUID.randomUUID());
        owner.setItemInHand(InteractionHand.MAIN_HAND, stale);
        owner.setShiftKeyDown(true);
        InteractionResult staleResult = owner.gameMode.useItem(
                owner, helper.getLevel(), stale, InteractionHand.MAIN_HAND);
        helper.assertTrue(staleResult instanceof InteractionResult.Success,
                "owner shift-use must handle a stale linked harness");
        helper.assertTrue(!stale.has(ModComponents.HARNESS_LINK),
                "owner shift-use must clear only the stale item link");

        ItemStack foreign = linkedHarness(owner.getUUID(), UUID.randomUUID());
        HarnessLink foreignLink = foreign.get(ModComponents.HARNESS_LINK);
        foreignPlayer.setItemInHand(InteractionHand.MAIN_HAND, foreign);
        foreignPlayer.setShiftKeyDown(true);
        InteractionResult foreignResult = foreignPlayer.gameMode.useItem(
                foreignPlayer, helper.getLevel(), foreign, InteractionHand.MAIN_HAND);
        helper.assertTrue(foreignResult instanceof InteractionResult.Fail,
                "foreign shift-use must be rejected");
        helper.assertTrue(foreignLink.equals(foreign.get(ModComponents.HARNESS_LINK)),
                "foreign rejection must retain the item link");

        UUID occupiedPetId = UUID.randomUUID();
        ItemStack occupied = linkedHarness(owner.getUUID(), occupiedPetId);
        HarnessLink occupiedLink = occupied.get(ModComponents.HARNESS_LINK);
        occupied.set(ModComponents.CAPTURED_PET, new CapturedPetData(
                CapturedPetData.CURRENT_VERSION, occupiedPetId, owner.getUUID(), new CompoundTag()));
        occupied.set(ModComponents.PET_RENDER_DATA, new PetRenderData(
                PetRenderData.CURRENT_SCHEMA, occupiedPetId,
                Identifier.withDefaultNamespace("wolf/wolf"), Optional.empty(), ItemStack.EMPTY));
        owner.setItemInHand(InteractionHand.MAIN_HAND, occupied);
        InteractionResult occupiedResult = owner.gameMode.useItem(
                owner, helper.getLevel(), occupied, InteractionHand.MAIN_HAND);
        helper.assertTrue(occupiedResult instanceof InteractionResult.Fail,
                "occupied shift-use must be rejected");
        helper.assertTrue(occupiedLink.equals(occupied.get(ModComponents.HARNESS_LINK))
                        && occupied.has(ModComponents.CAPTURED_PET)
                        && occupied.has(ModComponents.PET_RENDER_DATA),
                "occupied rejection must retain link, payload, and render authority");

        removePlayer(helper, foreignPlayer);
        removePlayer(helper, owner);
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void foreignLinkedHarnessCannotEquip(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        harness.set(ModComponents.HARNESS_LINK, new HarnessLink(UUID.randomUUID(), UUID.randomUUID()));
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);

        InteractionResult result = player.gameMode.useItem(
                player, helper.getLevel(), harness, InteractionHand.MAIN_HAND);

        helper.assertTrue(result instanceof InteractionResult.Fail,
                "a linked harness may only equip for its recorded owner");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                        && player.getMainHandItem() == harness,
                "owner rejection must be mutation-free");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void occupiedHarnessCannotEquipFromHand(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        UUID petId = UUID.randomUUID();
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        harness.set(ModComponents.HARNESS_LINK, new HarnessLink(player.getUUID(), petId));
        harness.set(ModComponents.CAPTURED_PET, new CapturedPetData(
                CapturedPetData.CURRENT_VERSION, petId, player.getUUID(), new CompoundTag()));
        harness.set(ModComponents.PET_RENDER_DATA, new PetRenderData(
                PetRenderData.CURRENT_SCHEMA, petId, Identifier.withDefaultNamespace("wolf/wolf"),
                Optional.empty(), ItemStack.EMPTY));
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);

        InteractionResult result = player.gameMode.useItem(
                player, helper.getLevel(), harness, InteractionHand.MAIN_HAND);

        helper.assertTrue(result instanceof InteractionResult.Fail,
                "an occupied authority found outside LEGS must fail closed, never copy or swap");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                        && player.getMainHandItem() == harness,
                "occupied rejection must leave the sole authority untouched in hand");
        helper.succeed();
    }

    private static ItemStack linkedHarness(UUID ownerId, UUID petId) {
        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        harness.set(ModComponents.HARNESS_LINK, new HarnessLink(ownerId, petId));
        return harness;
    }

    private static int countHarnesses(ServerPlayer player) {
        java.util.Set<ItemStack> stacks = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<>());
        stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            stacks.add(player.getInventory().getItem(slot));
        }
        return (int) stacks.stream().filter(stack -> stack.is(ModItems.PET_HARNESS)).count();
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }
}