package io.github.athysrock.petharnesses.transaction;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;

public final class SafeWolfPlacement {
    private SafeWolfPlacement() {}

    public static Optional<Vec3> find(ServerLevel level, BlockPos origin) {
        int[] verticalOffsets = {0, 1, -1, 2, -2};
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : verticalOffsets) {
                        Vec3 safe = DismountHelper.findSafeDismountLocation(
                                EntityTypes.WOLF, level, origin.offset(dx, dy, dz), true);
                        if (safe != null) return Optional.of(safe);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static boolean validatesLoadedWolf(ServerLevel level, Wolf wolf) {
        return level.getWorldBorder().isWithinBounds(wolf.getBoundingBox())
                && level.noCollision(wolf, wolf.getBoundingBox());
    }
}
