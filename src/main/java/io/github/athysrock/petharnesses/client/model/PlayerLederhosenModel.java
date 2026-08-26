package io.github.athysrock.petharnesses.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Native torso/leg equipment model for the cosmetic short-lederhosen harness. */
public final class PlayerLederhosenModel extends HumanoidModel<AvatarRenderState> {
    private static final CubeDeformation OUTER = new CubeDeformation(0.40F);
    private static final CubeDeformation TRIM = new CubeDeformation(0.50F);

    public PlayerLederhosenModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild(
                "right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild(
                "left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        body.addOrReplaceChild("raised_waistband",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, 9.5F, -2.0F, 8.0F, 2.05F, 4.0F, OUTER),
                PartPose.ZERO);
        body.addOrReplaceChild("waistband_highlight",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, 9.45F, -2.85F, 8.0F, 0.45F, 0.35F),
                PartPose.ZERO);
        body.addOrReplaceChild("front_fall_bib",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.5F, 6.0F, -3.0F, 5.0F, 3.35F, 0.50F),
                PartPose.ZERO);
        body.addOrReplaceChild("right_suspender",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(1.85F, -0.30F, -2.90F, 1.0F, 6.20F, 0.35F),
                PartPose.ZERO);
        body.addOrReplaceChild("left_suspender",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.85F, -0.30F, -2.90F, 1.0F, 6.20F, 0.35F),
                PartPose.ZERO);
        body.addOrReplaceChild("chest_bridge",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.80F, 3.5F, -2.90F, 3.60F, 1.25F, 0.35F),
                PartPose.ZERO);
        body.addOrReplaceChild("brass_hardware",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(1.95F, 3.65F, -3.35F, 0.8F, 0.9F, 0.25F)
                        .addBox(-2.75F, 3.65F, -3.35F, 0.8F, 0.9F, 0.25F)
                        .addBox(1.35F, 8.0F, -3.35F, 0.65F, 0.65F, 0.25F)
                        .addBox(-2.0F, 8.0F, -3.35F, 0.65F, 0.65F, 0.25F),
                PartPose.ZERO);
        body.addOrReplaceChild("cream_piping",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-2.45F, 6.3F, -3.30F, 0.25F, 2.8F, 0.20F)
                        .addBox(2.20F, 6.3F, -3.30F, 0.25F, 2.8F, 0.20F),
                PartPose.ZERO);

        rightLeg.addOrReplaceChild("right_short_cuff",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.0F, 0.45F, -2.0F, 3.45F, 4.15F, 4.0F, OUTER)
                        .texOffs(32, 0)
                        .addBox(-2.0F, 4.0F, -2.0F, 3.25F, 0.75F, 4.0F, TRIM)
                        .texOffs(16, 16)
                        .addBox(-1.55F, 3.55F, -2.80F, 0.3F, 0.35F, 0.20F),
                PartPose.ZERO);
        leftLeg.addOrReplaceChild("left_short_cuff",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.45F, 0.45F, -2.0F, 3.45F, 4.15F, 4.0F, OUTER)
                        .texOffs(32, 0)
                        .addBox(-1.25F, 4.0F, -2.0F, 3.25F, 0.75F, 4.0F, TRIM)
                        .texOffs(16, 16)
                        .addBox(1.25F, 3.55F, -2.80F, 0.3F, 0.35F, 0.20F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }
}
