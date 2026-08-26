package io.github.athysrock.petharnesses;

import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModAttachments;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.item.ModItems;
import io.github.athysrock.petharnesses.transaction.CaptureResult;
import io.github.athysrock.petharnesses.transaction.RestoreResult;
import io.github.athysrock.petharnesses.transaction.WolfCaptureTransaction;
import io.github.athysrock.petharnesses.transaction.WolfRestoreTransaction;
import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import io.netty.buffer.Unpooled;

/** Real 26.2 integration coverage for linking, lifecycle capture/restore, UUIDs and death. */
public final class PetHarnessesGameTests {
    @GameTest(maxTicks = 40)
    @SuppressWarnings("removal")
    public void occupiedPayloadIsAbsentFromPacketsAndCreativeEditsFailClosed(GameTestHelper helper) {
        String payloadSentinel = "PH_CAPTURED_NBT_SENTINEL_9f5a0e5c";
        Fixture fixture = occupiedFixture(helper, payloadSentinel);
        try {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, fixture.harness());
            assertBytesOmit(helper, buffer, payloadSentinel, "ordinary ItemStack stream codec");
            ItemStack clientProjection = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            helper.assertTrue(!clientProjection.has(ModComponents.CAPTURED_PET),
                    "common ItemStack packet codec must omit the full payload component");
            helper.assertTrue(clientProjection.has(ModComponents.HARNESS_LINK),
                    "client projection must retain stable link identity");
            helper.assertTrue(clientProjection.has(ModComponents.PET_RENDER_DATA),
                    "client projection must retain compact render data");

            ItemStackTemplate fromStack = ItemStackTemplate.fromStack(fixture.harness());
            ItemStackTemplate fromNonEmptyStack = ItemStackTemplate.fromNonEmptyStack(fixture.harness());
            assertTemplateProjection(helper, fromStack, "fromStack");
            assertTemplateProjection(helper, fromNonEmptyStack, "fromNonEmptyStack");

            RegistryFriendlyByteBuf templateBuffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            ItemStackTemplate.STREAM_CODEC.encode(templateBuffer, fromStack);
            assertBytesOmit(helper, templateBuffer, payloadSentinel, "ItemStackTemplate stream codec");
            assertTemplateProjection(helper, ItemStackTemplate.STREAM_CODEC.decode(templateBuffer),
                    "decoded ItemStackTemplate stream codec");

            var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
            var encodedTemplate = ItemStackTemplate.MAP_CODEC.codec()
                    .encodeStart(ops, fromStack).getOrThrow();
            helper.assertTrue(!encodedTemplate.toString().contains(payloadSentinel),
                    "ItemStackTemplate map codec must omit captured NBT marker");
            assertTemplateProjection(helper,
                    ItemStackTemplate.MAP_CODEC.codec().parse(ops, encodedTemplate).getOrThrow(),
                    "decoded ItemStackTemplate map codec");

            RegistryFriendlyByteBuf patchBuffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            DataComponentPatch.STREAM_CODEC.encode(
                    patchBuffer, fixture.harness().getComponentsPatch());
            assertBytesOmit(helper, patchBuffer, payloadSentinel, "direct component patch stream codec");
            DataComponentPatch decodedPatch = DataComponentPatch.STREAM_CODEC.decode(patchBuffer);
            helper.assertTrue(decodedPatch.entrySet().stream()
                            .noneMatch(entry -> entry.getKey() == ModComponents.CAPTURED_PET),
                    "direct component patch stream codec must omit captured component");
            helper.assertTrue(decodedPatch.entrySet().stream()
                            .anyMatch(entry -> entry.getKey() == ModComponents.HARNESS_LINK),
                    "direct component patch stream codec must retain harness link");
            helper.assertTrue(decodedPatch.entrySet().stream()
                            .anyMatch(entry -> entry.getKey() == ModComponents.PET_RENDER_DATA),
                    "direct component patch stream codec must retain render data");

            RegistryFriendlyByteBuf componentBuffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            ComponentSerialization.STREAM_CODEC.encode(
                    componentBuffer, fixture.harness().getDisplayName());
            assertBytesOmit(helper, componentBuffer, payloadSentinel,
                    "real display-name component stream codec");
            Component decodedDisplayName = ComponentSerialization.STREAM_CODEC.decode(componentBuffer);
            HoverEvent hoverEvent = decodedDisplayName.getStyle().getHoverEvent();
            helper.assertTrue(hoverEvent instanceof HoverEvent.ShowItem,
                    "decoded display name must retain its show-item hover event");
            assertTemplateProjection(helper, ((HoverEvent.ShowItem) hoverEvent).item(),
                    "decoded display-name hover template");

            fixture.player().setGameMode(GameType.CREATIVE);
            int legsMenuSlot = -1;
            for (int slot = 1; slot <= 45; slot++) {
                if (fixture.player().inventoryMenu.getSlot(slot).getItem() == fixture.harness()) {
                    legsMenuSlot = slot;
                    break;
                }
            }
            helper.assertTrue(legsMenuSlot >= 1, "real inventory menu must expose occupied LEGS slot");
            fixture.player().connection.handleSetCreativeModeSlot(
                    new ServerboundSetCreativeModeSlotPacket(legsMenuSlot, ItemStack.EMPTY));
            helper.assertTrue(fixture.player().getItemBySlot(EquipmentSlot.LEGS) == fixture.harness(),
                    "creative removal packet must not replace authoritative occupied slot");
            helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET),
                    "creative removal packet must retain complete payload");

            int emptyMenuSlot = 9;
            fixture.player().inventoryMenu.getSlot(emptyMenuSlot).set(ItemStack.EMPTY);
            fixture.player().connection.handleSetCreativeModeSlot(
                    new ServerboundSetCreativeModeSlotPacket(emptyMenuSlot, clientProjection));
            helper.assertTrue(fixture.player().inventoryMenu.getSlot(emptyMenuSlot).getItem().isEmpty(),
                    "creative copy packet must not install occupied link/render projection");
        } finally {
            removePlayer(helper, fixture.player());
        }
        helper.succeed();
    }

    private static void assertTemplateProjection(
            GameTestHelper helper, ItemStackTemplate template, String boundary) {
        helper.assertTrue(template.components().entrySet().stream()
                        .noneMatch(entry -> entry.getKey() == ModComponents.CAPTURED_PET),
                boundary + " must omit captured pet authority");
        helper.assertTrue(template.components().entrySet().stream()
                        .anyMatch(entry -> entry.getKey() == ModComponents.HARNESS_LINK),
                boundary + " must retain harness link");
        helper.assertTrue(template.components().entrySet().stream()
                        .anyMatch(entry -> entry.getKey() == ModComponents.PET_RENDER_DATA),
                boundary + " must retain render data");
    }

    private static void assertBytesOmit(
            GameTestHelper helper, RegistryFriendlyByteBuf buffer, String sentinel, String boundary) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), bytes);
        helper.assertTrue(!new String(bytes, StandardCharsets.ISO_8859_1).contains(sentinel),
                boundary + " must not contain the distinctive captured-NBT marker");
    }

    @GameTest(maxTicks = 12)
    @SuppressWarnings("removal")
    public void recipientInventoryExitRestoresUsingOwnerAndActualStack(GameTestHelper helper) {
        Fixture fixture = occupiedFixture(helper, "Two Player Wolf");
        ServerPlayer recipient = helper.makeMockServerPlayerInLevel();
        recipient.snapTo(helper.absoluteVec(new Vec3(7.5, 2.0, 7.5)), 0.0F, 0.0F);
        recipient.getInventory().setItem(0, fixture.harness());
        fixture.player().setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        if (fixture.harness().has(ModComponents.CAPTURED_PET)) {
            RestoreResult result = WolfRestoreTransaction.restore(
                    fixture.player(), fixture.harness());
            helper.assertTrue(result == RestoreResult.SUCCESS,
                    "recipient-held actual stack must restore using its recorded owner, got " + result);
        }

        helper.runAtTickTime(4, () -> {
            try {
                helper.assertTrue(!fixture.harness().has(ModComponents.CAPTURED_PET),
                        "actual stack in recipient inventory must be cleared after restore");
                Wolf restored = (Wolf) helper.getLevel().getEntity(fixture.link().petId());
                helper.assertTrue(restored != null, "global locator must restore recipient-held stack");
                helper.assertTrue(restored.isOwnedBy(fixture.player()),
                        "recorded owner must be restore owner; recipient must never substitute");
                helper.assertTrue(!restored.isOwnedBy(recipient),
                        "recipient must not become restored wolf owner");
            } finally {
                removePlayer(helper, recipient);
                removePlayer(helper, fixture.player());
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 12)
    @SuppressWarnings("removal")
    public void duplicateOccupiedStacksAcrossPlayersFailClosed(GameTestHelper helper) {
        Fixture fixture = occupiedFixture(helper, "Duplicate Stack Wolf");
        ServerPlayer recipient = helper.makeMockServerPlayerInLevel();
        recipient.snapTo(helper.absoluteVec(new Vec3(7.5, 2.0, 7.5)), 0.0F, 0.0F);
        ItemStack duplicate = fixture.harness().copy();
        recipient.getInventory().setItem(0, fixture.harness());
        recipient.getInventory().setItem(1, duplicate);
        fixture.player().setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);

        helper.runAtTickTime(4, () -> {
            try {
                helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET),
                        "first duplicate must retain its authoritative payload");
                helper.assertTrue(duplicate.has(ModComponents.CAPTURED_PET),
                        "second duplicate must retain its authoritative payload");
                helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) == null,
                        "duplicate representations must not fabricate or restore a wolf");
            } finally {
                removePlayer(helper, recipient);
                removePlayer(helper, fixture.player());
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 10)
    @SuppressWarnings("removal")
    public void actualLinkAndEquipmentLifecycleRoundTripCompleteWolfState(GameTestHelper helper) {
        Fixture fixture = linkedFixture(helper, "Harness GameTest Wolf");
        fixture.player().setItemSlot(EquipmentSlot.LEGS, fixture.harness());
        helper.runAtTickTime(2, () -> {
            assertOccupied(helper, fixture);
            fixture.player().getInventory().setItem(0, fixture.harness());
            fixture.player().setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        });
        helper.runAtTickTime(4, () -> {
            try {
                Wolf restored = (Wolf) helper.getLevel().getEntity(fixture.link().petId());
                helper.assertTrue(restored != null, "equipment exit must restore the wolf before clearing NBT");
                helper.assertTrue(!fixture.harness().has(ModComponents.CAPTURED_PET),
                        "verified insertion must clear full NBT");
                helper.assertTrue("Harness GameTest Wolf".equals(restored.getCustomName().getString()),
                        "custom name must round-trip");
                helper.assertTrue(restored.getHealth() == 7.0F, "health must round-trip");
                helper.assertTrue(restored.getBodyArmorItem().is(Items.WOLF_ARMOR),
                        "body armor item must round-trip");
                helper.assertTrue(restored.getBodyArmorItem().getDamageValue() == 11,
                        "body armor damage must round-trip");
                helper.assertTrue(restored.isOwnedBy(fixture.player()), "owner identity must round-trip");
                helper.assertTrue(fixture.harness().get(ModComponents.HARNESS_LINK)
                                .equals(restored.getAttached(ModAttachments.PET_LINK)),
                        "reciprocal link must round-trip");
            } finally {
                removePlayer(helper, fixture.player());
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    @SuppressWarnings("removal")
    public void duplicateUuidFailsClosedAndRetainsPayload(GameTestHelper helper) {
        Fixture fixture = occupiedFixture(helper, "Duplicate UUID Wolf");
        Wolf conflict = null;
        try {
            conflict = EntityTypes.WOLF.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            helper.assertTrue(conflict != null, "test conflict wolf must construct");
            conflict.setUUID(fixture.link().petId());
            conflict.snapTo(helper.absoluteVec(new Vec3(7.5, 2.0, 7.5)), 0.0F, 0.0F);
            helper.assertTrue(helper.getLevel().addFreshEntity(conflict), "test UUID conflict must insert");

            RestoreResult result = WolfRestoreTransaction.restore(fixture.player(), fixture.harness());
            helper.assertTrue(result == RestoreResult.DUPLICATE_UUID, "conflicting UUID must fail closed");
            helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET),
                    "UUID conflict must retain complete payload");
        } finally {
            if (conflict != null) conflict.discard();
            removePlayer(helper, fixture.player());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    @SuppressWarnings("removal")
    public void droppingDeathRestoresBeforeInventoryDrops(GameTestHelper helper) {
        Fixture fixture = occupiedFixture(helper, "Dropping Death Wolf");
        try {
            helper.getLevel().getGameRules().set(
                    GameRules.KEEP_INVENTORY, false, helper.getLevel().getServer());
            fixture.player().die(fixture.player().damageSources().generic());

            helper.assertTrue(!fixture.harness().has(ModComponents.CAPTURED_PET),
                    "dropping death must clear only after restore");
            helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) instanceof Wolf,
                    "dropping death must restore the exact UUID before loot processing");
        } finally {
            removePlayer(helper, fixture.player());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    @SuppressWarnings("removal")
    public void realEntityDispatchLinksHarnessBeforeVanillaWolfSitToggle(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(4.5, 2.0, 4.5)), 0.0F, 0.0F);
        Wolf wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(4, 2, 4));
        wolf.tame(player);
        wolf.setAge(0);
        wolf.setOrderedToSit(true);

        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        player.setItemInHand(InteractionHand.MAIN_HAND, harness);
        for (int i = 0; i < 60; i++) player.connection.tickClientLoadTimeout();
        helper.assertTrue(player.connection.hasClientLoaded(),
                "packet-dispatch fixture must satisfy the mock client-load gate");
        player.connection.handleInteract(new ServerboundInteractPacket(
                wolf.getId(), InteractionHand.MAIN_HAND, Vec3.ZERO, false));
        HarnessLink link = harness.get(ModComponents.HARNESS_LINK);
        helper.assertTrue(link != null && link.equals(wolf.getAttached(ModAttachments.PET_LINK)),
                "real packet dispatch must create matching reciprocal harness state");
        helper.assertTrue(wolf.isOrderedToSit(),
                "harness interaction must cancel vanilla owner sit-toggle processing");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.connection.handleInteract(new ServerboundInteractPacket(
                wolf.getId(), InteractionHand.MAIN_HAND, Vec3.ZERO, false));
        helper.assertTrue(!wolf.isOrderedToSit(),
                "unrelated items must retain vanilla wolf sit-toggle behavior");
        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    @SuppressWarnings("removal")
    public void keepInventoryDeathRetainsOccupiedHarness(GameTestHelper helper) {
        Fixture fixture = occupiedFixture(helper, "Keep Inventory Wolf");
        try {
            helper.getLevel().getGameRules().set(
                    GameRules.KEEP_INVENTORY, true, helper.getLevel().getServer());
            fixture.player().die(fixture.player().damageSources().generic());

            helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET),
                    "keepInventory death must retain the occupied payload");
            helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) == null,
                    "keepInventory death must not materialize the captured wolf");
        } finally {
            helper.getLevel().getGameRules().set(
                    GameRules.KEEP_INVENTORY, false, helper.getLevel().getServer());
            removePlayer(helper, fixture.player());
        }
        helper.succeed();
    }

    private static Fixture occupiedFixture(GameTestHelper helper, String name) {
        Fixture fixture = linkedFixture(helper, name);
        fixture.player().setItemSlot(EquipmentSlot.LEGS, fixture.harness());
        if (!fixture.harness().has(ModComponents.CAPTURED_PET)) {
            CaptureResult result = WolfCaptureTransaction.capture(fixture.player(), fixture.harness());
            helper.assertTrue(result == CaptureResult.SUCCESS, "real wolf capture must commit");
        }
        assertOccupied(helper, fixture);
        return fixture;
    }

    private static Fixture linkedFixture(GameTestHelper helper, String name) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(4.5, 2.0, 4.5)), 0.0F, 0.0F);

        Wolf wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(4, 2, 4));
        wolf.tame(player);
        wolf.setAge(0);
        wolf.setHealth(7.0F);
        wolf.setCustomName(Component.literal(name));
        ItemStack armor = new ItemStack(Items.WOLF_ARMOR);
        armor.setDamageValue(11);
        wolf.setItemSlot(EquipmentSlot.BODY, armor);

        ItemStack harness = new ItemStack(ModItems.PET_HARNESS);
        InteractionResult interaction = ModItems.PET_HARNESS.interactLivingEntity(
                harness, player, wolf, InteractionHand.MAIN_HAND);
        helper.assertTrue(interaction == InteractionResult.SUCCESS, "real item interaction must link");
        HarnessLink link = harness.get(ModComponents.HARNESS_LINK);
        helper.assertTrue(link != null, "linked item must contain reciprocal identity");
        helper.assertTrue(link.equals(wolf.getAttached(ModAttachments.PET_LINK)),
                "wolf attachment must match linked item");
        return new Fixture(player, wolf, harness, link);
    }

    private static void assertOccupied(GameTestHelper helper, Fixture fixture) {
        helper.assertTrue(fixture.harness().has(ModComponents.CAPTURED_PET),
                "equipment lifecycle must retain full NBT");
        helper.assertTrue(fixture.harness().has(ModComponents.PET_RENDER_DATA),
                "equipment lifecycle must retain compact render data");
        helper.assertTrue(helper.getLevel().getEntity(fixture.link().petId()) == null,
                "captured UUID must leave the world");
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private record Fixture(ServerPlayer player, Wolf wolf, ItemStack harness, HarnessLink link) {}
}
