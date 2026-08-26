package io.github.athysrock.petharnesses.lifecycle;

import com.mojang.logging.LogUtils;
import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.item.ModItems;
import io.github.athysrock.petharnesses.transaction.RestoreResult;
import io.github.athysrock.petharnesses.transaction.WolfCaptureTransaction;
import io.github.athysrock.petharnesses.transaction.WolfRestoreTransaction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/** Equipment event fast path plus end-tick retry/reconciliation for every LEGS mutation route. */
public final class HarnessEquipmentLifecycle {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, LegsSnapshot> SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, PendingExit> PENDING_EXITS = new HashMap<>();
    private static final List<Consumer<ServerPlayer>> RESTORE_OBSERVERS = new CopyOnWriteArrayList<>();

    private HarnessEquipmentLifecycle() {}

    public static void register() {
        ServerEntityEvents.EQUIPMENT_CHANGE.register(HarnessEquipmentLifecycle::onEquipmentChange);
        ServerTickEvents.END_SERVER_TICK.register(HarnessEquipmentLifecycle::onEndTick);
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            reset(oldPlayer);
            reset(newPlayer);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> reset(newPlayer));
    }

    public static boolean restoreBeforeDroppingDeath(ServerPlayer player) {
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!isOccupied(legs)) return true;
        RestoreResult result = WolfRestoreTransaction.restore(player, legs);
        if (result == RestoreResult.SUCCESS) return true;
        LOGGER.error("Cancelled death for {} because occupied harness restore failed: {}",
                player.getScoreboardName(), result);
        return false;
    }

    static Runnable observeRestoresForTest(Consumer<ServerPlayer> observer) {
        Consumer<ServerPlayer> required = java.util.Objects.requireNonNull(observer, "observer");
        RESTORE_OBSERVERS.add(required);
        return () -> RESTORE_OBSERVERS.remove(required);
    }

    private static void onEquipmentChange(
            LivingEntity entity, EquipmentSlot slot, ItemStack previous, ItemStack current) {
        if (!(entity instanceof ServerPlayer player) || slot != EquipmentSlot.LEGS) return;
        HarnessLink previousLink = previous.get(ModComponents.HARNESS_LINK);
        if (isOccupied(previous) && !sameOccupiedHarness(previous, current)) {
            restoreMoved(player, previousLink, RestoreTrigger.EQUIPMENT_CHANGE);
        }
        if (isLinked(current)) captureEquipped(player, current);
        SNAPSHOTS.put(player.getUUID(), snapshot(player.getItemBySlot(EquipmentSlot.LEGS)));
    }

    private static void onEndTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PendingExit pending = PENDING_EXITS.get(player.getUUID());
            if (pending != null) restoreMoved(player, pending.link(), RestoreTrigger.PENDING_RETRY);

            ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
            LegsSnapshot previous = SNAPSHOTS.get(player.getUUID());
            if (previous != null && previous.occupied()
                    && (!isOccupied(legs) || !previous.link().equals(legs.get(ModComponents.HARNESS_LINK)))) {
                restoreMoved(player, previous.link(), RestoreTrigger.END_TICK_SNAPSHOT);
            }
            if (isLinked(legs)) {
                captureEquipped(player, legs);
            } else if (isOccupied(legs)) {
                WolfCaptureTransaction.reconcileOccupied(player, legs);
            }
            SNAPSHOTS.put(player.getUUID(), snapshot(player.getItemBySlot(EquipmentSlot.LEGS)));
        }
    }

    private static void captureEquipped(ServerPlayer player, ItemStack stack) {
        WolfCaptureTransaction.capture(player, stack);
    }

    private static void restoreMoved(ServerPlayer player, HarnessLink link, RestoreTrigger trigger) {
        if (link == null) return;
        ItemStack authoritativeLegs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (isOccupied(authoritativeLegs)
                && link.equals(authoritativeLegs.get(ModComponents.HARNESS_LINK))) {
            PENDING_EXITS.remove(player.getUUID());
            LOGGER.debug("Ignored {} restore signal for {} because occupied harness {} remains in LEGS",
                    trigger, player.getScoreboardName(), link.linkId());
            return;
        }
        var matches = HarnessStackLocator.find(player.level().getServer(), link);
        if (matches.size() != 1) {
            PENDING_EXITS.put(player.getUUID(), new PendingExit(link));
            return;
        }
        HarnessStackLocator.LocatedHarness located = matches.getFirst();
        ItemStack stack = located.stack();
        LOGGER.info("Restoring harness {} pet {} for {} after {} from {}",
                link.linkId(), link.petId(), player.getScoreboardName(), trigger, located.kind());
        RESTORE_OBSERVERS.forEach(observer -> observer.accept(player));
        RestoreResult result = WolfRestoreTransaction.restore(player, stack);
        if (result == RestoreResult.SUCCESS) {
            PENDING_EXITS.remove(player.getUUID());
        } else {
            PENDING_EXITS.put(player.getUUID(), new PendingExit(link));
        }
    }

    private static boolean isLinked(ItemStack stack) {
        return stack.is(ModItems.PET_HARNESS)
                && stack.has(ModComponents.HARNESS_LINK)
                && !stack.has(ModComponents.CAPTURED_PET);
    }

    private static boolean isOccupied(ItemStack stack) {
        return HarnessStackLocator.isCanonicalOccupied(stack);
    }

    private static boolean sameOccupiedHarness(ItemStack first, ItemStack second) {
        HarnessLink firstLink = first.get(ModComponents.HARNESS_LINK);
        return firstLink != null && isOccupied(second)
                && firstLink.equals(second.get(ModComponents.HARNESS_LINK));
    }

    private static LegsSnapshot snapshot(ItemStack stack) {
        return new LegsSnapshot(isOccupied(stack), stack.get(ModComponents.HARNESS_LINK));
    }

    private static void reset(ServerPlayer player) {
        SNAPSHOTS.remove(player.getUUID());
        PENDING_EXITS.remove(player.getUUID());
    }

    private record LegsSnapshot(boolean occupied, HarnessLink link) {}
    private record PendingExit(HarnessLink link) {}
    private enum RestoreTrigger { EQUIPMENT_CHANGE, END_TICK_SNAPSHOT, PENDING_RETRY }
}
