package io.github.athysrock.petharnesses.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.athysrock.petharnesses.PetHarnesses;
import io.github.athysrock.petharnesses.client.CarriedWolfMotionSnapshot;
import io.github.athysrock.petharnesses.client.ClientRenderDataKeys;
import io.github.athysrock.petharnesses.client.PetHarnessesClient;
import io.github.athysrock.petharnesses.client.model.CarriedWolfModel;
import io.github.athysrock.petharnesses.client.model.CarriedWolfRenderState;
import io.github.athysrock.petharnesses.component.PetRenderData;
import io.github.athysrock.petharnesses.physics.LegAngles;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Player-front full-scale adult wolf, driven entirely from compact equipped-stack render data. */
public final class CarriedWolfRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier COLLAR_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/wolf/wolf_collar.png");
    private static final Identifier HARNESS_TEXTURE =
            PetHarnesses.id("textures/entity/carried_wolf_square_i_harness.png");
    private final CarriedWolfModel model;
    private final CarriedWolfModel harnessModel;
    private final CarriedWolfRenderPhysics renderPhysics = new CarriedWolfRenderPhysics();

    public CarriedWolfRenderLayer(
            RenderLayerParent<AvatarRenderState, PlayerModel> parent,
            EntityRendererProvider.Context context) {
        super(parent);
        this.model = new CarriedWolfModel(context.bakeLayer(PetHarnessesClient.CARRIED_WOLF_LAYER));
        this.model.setHarnessVisible(false);
        this.harnessModel = new CarriedWolfModel(context.bakeLayer(PetHarnessesClient.CARRIED_WOLF_LAYER));
        this.harnessModel.setWolfVisible(false);
        this.harnessModel.setHarnessVisible(true);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            AvatarRenderState playerState,
            float yRot,
            float xRot) {
        PetRenderData data = playerState.getData(ClientRenderDataKeys.PET);
        CarriedWolfMotionSnapshot motion = playerState.getData(ClientRenderDataKeys.MOTION);
        if (data == null || motion == null || playerState.isInvisible) return;

        double partialTicks = playerState.ageInTicks - Math.floor(playerState.ageInTicks);
        LegAngles angles = renderPhysics.sample(
                motion.playerEntityId(), data.petId(), motion.gameTick(), partialTicks, motion.input());

        CarriedWolfRenderState wolfState = new CarriedWolfRenderState();
        wolfState.texture = data.texture();
        wolfState.collarColor = data.collarColor().orElse(null);
        wolfState.bodyArmorItem = data.bodyArmor();
        wolfState.isBaby = false;
        wolfState.ageScale = 1.0F;
        wolfState.scale = 1.0F;
        wolfState.outlineColor = playerState.outlineColor;
        wolfState.rightFrontAngle = (float) angles.rightFront();
        wolfState.leftFrontAngle = (float) angles.leftFront();
        wolfState.rightHindAngle = (float) angles.rightHind();
        wolfState.leftHindAngle = (float) angles.leftHind();

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        submitNodeCollector.submitModel(
                model, wolfState, poseStack, RenderTypes.entityCutout(data.texture()),
                lightCoords, OverlayTexture.NO_OVERLAY, playerState.outlineColor, null);
        if (wolfState.collarColor != null) {
            submitNodeCollector.order(1).submitModel(
                    model, wolfState, poseStack, RenderTypes.entityCutout(COLLAR_TEXTURE),
                    lightCoords, OverlayTexture.NO_OVERLAY,
                    wolfState.collarColor.getTextureDiffuseColor(), null,
                    playerState.outlineColor, null);
        }
        submitNodeCollector.order(2).submitModel(
                harnessModel, wolfState, poseStack, RenderTypes.entityCutout(HARNESS_TEXTURE),
                lightCoords, OverlayTexture.NO_OVERLAY, playerState.outlineColor, null);
        poseStack.popPose();
    }
}
