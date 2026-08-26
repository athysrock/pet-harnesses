package io.github.athysrock.petharnesses.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.athysrock.petharnesses.PetHarnesses;
import io.github.athysrock.petharnesses.client.ClientRenderDataKeys;
import io.github.athysrock.petharnesses.client.PetHarnessesClient;
import io.github.athysrock.petharnesses.client.model.PlayerLederhosenModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Cosmetic equipment layer shown for the Pet Harness in LEGS, independent of skin arm width. */
public final class PlayerLederhosenRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier TEXTURE =
            PetHarnesses.id("textures/entity/lederhosen_harness.png");
    private final PlayerLederhosenModel model;

    public PlayerLederhosenRenderLayer(
            RenderLayerParent<AvatarRenderState, PlayerModel> parent,
            EntityRendererProvider.Context context) {
        super(parent);
        this.model = new PlayerLederhosenModel(context.bakeLayer(PetHarnessesClient.PLAYER_LEDERHOSEN_LAYER));
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            AvatarRenderState state,
            float yRot,
            float xRot) {
        if (!Boolean.TRUE.equals(state.getData(ClientRenderDataKeys.HARNESS_EQUIPPED)) || state.isInvisible) {
            return;
        }
        submitNodeCollector.submitModel(
                model, state, poseStack, RenderTypes.entityCutout(TEXTURE),
                lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }
}
