package io.github.athysrock.petharnesses.lifecycle;

import io.github.athysrock.petharnesses.component.CapturedPetData;
import io.github.athysrock.petharnesses.component.HarnessLink;
import io.github.athysrock.petharnesses.component.ModComponents;
import io.github.athysrock.petharnesses.component.PetRenderData;
import io.github.athysrock.petharnesses.item.ModItems;
import io.github.athysrock.petharnesses.transaction.EntityNbtValidator;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Globally scans every online/loaded stack-bearing location and preserves actual stack identity. */
public final class HarnessStackLocator {
    private HarnessStackLocator() {}

    public static List<LocatedHarness> find(MinecraftServer server, HarnessLink expected) {
        requireServerThread(server);
        return all(server).stream().filter(location -> matches(location.stack(), expected)).toList();
    }

    /** Every visible harness payload claiming this pet, including malformed/conflicting claims. */
    public static List<LocatedHarness> findOccupiedByPet(MinecraftServer server, UUID petId) {
        requireServerThread(server);
        return all(server).stream().filter(location -> {
            CapturedPetData captured = location.stack().get(ModComponents.CAPTURED_PET);
            return location.stack().is(ModItems.PET_HARNESS)
                    && captured != null && captured.petId().equals(petId);
        }).toList();
    }

    public static List<LocatedHarness> allOccupied(MinecraftServer server) {
        requireServerThread(server);
        return all(server).stream().filter(location -> isCanonicalOccupied(location.stack())).toList();
    }

    private static List<LocatedHarness> all(MinecraftServer server) {
        List<LocatedHarness> result = new ArrayList<>();
        Map<ItemStack, Boolean> seen = new IdentityHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
                add(player.getItemBySlot(equipmentSlot), LocationKind.EQUIPMENT, seen, result);
            }
            for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
                add(player.getInventory().getItem(index), LocationKind.INVENTORY, seen, result);
            }
            for (Slot slot : player.containerMenu.slots) {
                add(slot.getItem(), LocationKind.OPEN_MENU, seen, result);
            }
            add(player.containerMenu.getCarried(), LocationKind.CURSOR, seen, result);
        }
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    add(itemEntity.getItem(), LocationKind.ITEM_ENTITY, seen, result);
                }
            }
        }
        return List.copyOf(result);
    }

    private static void add(
            ItemStack stack,
            LocationKind kind,
            Map<ItemStack, Boolean> seen,
            List<LocatedHarness> result) {
        if (!stack.isEmpty() && seen.put(stack, Boolean.TRUE) == null) {
            result.add(new LocatedHarness(stack, kind));
        }
    }

    private static boolean matches(ItemStack stack, HarnessLink expected) {
        if (!isCanonicalOccupied(stack)) return false;
        CapturedPetData captured = stack.get(ModComponents.CAPTURED_PET);
        PetRenderData render = stack.get(ModComponents.PET_RENDER_DATA);
        return expected.equals(stack.get(ModComponents.HARNESS_LINK))
                && expected.petId().equals(captured.petId())
                && expected.petId().equals(render.petId());
    }

    public static boolean isCanonicalOccupied(ItemStack stack) {
        if (!stack.is(ModItems.PET_HARNESS)) return false;
        HarnessLink link = stack.get(ModComponents.HARNESS_LINK);
        CapturedPetData captured = stack.get(ModComponents.CAPTURED_PET);
        PetRenderData render = stack.get(ModComponents.PET_RENDER_DATA);
        return link != null && captured != null && render != null
                && link.ownerId().equals(captured.ownerId())
                && link.petId().equals(captured.petId())
                && link.petId().equals(render.petId())
                && EntityNbtValidator.isCanonicalAdultWolf(
                        captured.entityData(), link.petId(), link.ownerId());
    }

    private static void requireServerThread(MinecraftServer server) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("harness stack scan must run on the server thread");
        }
    }

    public enum LocationKind {
        EQUIPMENT,
        INVENTORY,
        OPEN_MENU,
        CURSOR,
        ITEM_ENTITY
    }

    public record LocatedHarness(ItemStack stack, LocationKind kind) {}
}
