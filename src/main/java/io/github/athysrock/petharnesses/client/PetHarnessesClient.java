package io.github.athysrock.petharnesses.client;

import io.github.athysrock.petharnesses.PetHarnesses;
import io.github.athysrock.petharnesses.client.model.CarriedWolfModel;
import io.github.athysrock.petharnesses.client.model.PlayerLederhosenModel;
import io.github.athysrock.petharnesses.client.render.CarriedWolfRenderLayer;
import io.github.athysrock.petharnesses.client.render.PlayerLederhosenRenderLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityTypes;

public final class PetHarnessesClient implements ClientModInitializer {
    public static final ModelLayerLocation CARRIED_WOLF_LAYER =
            new ModelLayerLocation(PetHarnesses.id("carried_wolf"), "main");
    public static final ModelLayerLocation PLAYER_LEDERHOSEN_LAYER =
            new ModelLayerLocation(PetHarnesses.id("player_lederhosen"), "main");

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(CARRIED_WOLF_LAYER, CarriedWolfModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(PLAYER_LEDERHOSEN_LAYER, PlayerLederhosenModel::createBodyLayer);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (entityType == EntityTypes.PLAYER && renderer instanceof AvatarRenderer avatarRenderer) {
                helper.register(new PlayerLederhosenRenderLayer(avatarRenderer, context));
                helper.register(new CarriedWolfRenderLayer(avatarRenderer, context));
            }
        });
    }
}
