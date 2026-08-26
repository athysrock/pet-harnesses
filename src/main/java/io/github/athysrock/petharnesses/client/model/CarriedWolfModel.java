package io.github.athysrock.petharnesses.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Full-size native ModelPart transcription of Athysrock's authored adult-wolf carry pose. */
public final class CarriedWolfModel extends EntityModel<CarriedWolfRenderState> {
    private final ModelPart source;
    private final ModelPart harness;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;

    public CarriedWolfModel(ModelPart root) {
        super(root);
        ModelPart carry = root.getChild("front_mount_root")
                .getChild("runtime_scale_root")
                .getChild("carry_pose");
        this.source = carry.getChild("source_root");
        this.harness = carry.getChild("harness_root");
        this.rightFrontLeg = source.getChild("right_front_leg");
        this.leftFrontLeg = source.getChild("left_front_leg");
        this.rightHindLeg = source.getChild("right_hind_leg");
        this.leftHindLeg = source.getChild("left_hind_leg");
    }

    @Override
    public void setupAnim(CarriedWolfRenderState state) {
        super.setupAnim(state);
        rightFrontLeg.xRot = state.rightFrontAngle;
        leftFrontLeg.xRot = state.leftFrontAngle;
        rightHindLeg.xRot = state.rightHindAngle;
        leftHindLeg.xRot = state.leftHindAngle;
    }

    public void setWolfVisible(boolean visible) {
        source.visible = visible;
    }

    public void setHarnessVisible(boolean visible) {
        harness.visible = visible;
    }

    public static LayerDefinition createBodyLayer() {
        CubeDeformation deformation = CubeDeformation.NONE;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition mount = root.addOrReplaceChild(
                "front_mount_root", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 8.9F, -6.9F, -(float) Math.PI / 2.0F, 0.0F, 0.0F));
        PartDefinition scale = mount.addOrReplaceChild(
                "runtime_scale_root", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition carry = scale.addOrReplaceChild(
                "carry_pose", CubeListBuilder.create(), PartPose.offset(0.0F, -14.0F, 0.0F));
        PartDefinition source = carry.addOrReplaceChild(
                "source_root", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition head = source.addOrReplaceChild(
                "head", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.0F, 13.5F, -7.0F,
                        11.0F * (float) Math.PI / 24.0F, 0.0F, 0.0F));
        head.addOrReplaceChild(
                "real_head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, deformation)
                        .texOffs(16, 14).addBox(-2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, deformation)
                        .texOffs(16, 14).addBox(2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, deformation)
                        .texOffs(0, 10).addBox(-0.5F, -0.001F, -5.0F, 3.0F, 3.0F, 4.0F, deformation),
                PartPose.ZERO);
        source.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(18, 14)
                        .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, deformation),
                PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, (float) Math.PI / 2.0F, 0.0F, 0.0F));
        source.addOrReplaceChild(
                "upper_body",
                CubeListBuilder.create().texOffs(21, 0)
                        .addBox(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, deformation),
                PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, (float) Math.PI / 2.0F, 0.0F, 0.0F));

        source.addOrReplaceChild(
                "right_hind_leg", rightLeg(deformation),
                PartPose.offsetAndRotation(-2.5F, 16.0F, 5.0F, (float) Math.PI / 3.0F, 0.0F, 0.0F));
        source.addOrReplaceChild(
                "left_hind_leg", leftLeg(deformation),
                PartPose.offsetAndRotation(0.5F, 16.0F, 5.0F, (float) Math.PI / 6.0F, 0.0F, 0.0F));
        source.addOrReplaceChild(
                "right_front_leg", rightLeg(deformation),
                PartPose.offsetAndRotation(-2.5F, 16.0F, -4.0F, 25.0F * (float) Math.PI / 72.0F, 0.0F, 0.0F));
        source.addOrReplaceChild(
                "left_front_leg", leftLeg(deformation),
                PartPose.offsetAndRotation(0.5F, 16.0F, -4.0F, (float) Math.PI / 8.0F, 0.0F, 0.0F));
        PartDefinition tail = source.addOrReplaceChild(
                "tail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.0F, 12.0F, 7.0F, (float) Math.PI / 5.0F, 0.0F, 0.0F));
        tail.addOrReplaceChild(
                "real_tail",
                CubeListBuilder.create().texOffs(9, 18)
                        .addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation),
                PartPose.ZERO);

        PartDefinition harness = carry.addOrReplaceChild(
                "harness_root", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F,
                        (float) Math.PI / 2.0F, 0.0F, 0.0F));
        harness.addOrReplaceChild(
                "upper_chest_strap",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.5F, -0.45F, -0.17F, 5.0F, 0.9F, 0.34F),
                PartPose.offset(1.0F, -1.35F, -3.45F));
        harness.addOrReplaceChild(
                "lower_stomach_strap",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -0.45F, -0.17F, 8.0F, 0.9F, 0.34F),
                PartPose.offset(1.0F, 1.35F, -3.45F));
        harness.addOrReplaceChild(
                "center_connector",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.45F, 0.0F, -0.17F, 0.9F, 2.0F, 0.34F),
                PartPose.offset(1.0F, -0.90F, -3.45F));
        harness.addOrReplaceChild(
                "center_cream_stitch",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-0.12F, 0.0F, -0.08F, 0.24F, 1.30F, 0.16F),
                PartPose.offset(1.0F, -0.55F, -3.70F));
        harness.addOrReplaceChild(
                "right_upper_side_wrap",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-0.15F, -0.45F, -3.35F, 0.3F, 0.9F, 6.7F),
                PartPose.offset(3.65F, -1.35F, 0.0F));
        harness.addOrReplaceChild(
                "left_upper_side_wrap",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-0.15F, -0.45F, -3.35F, 0.3F, 0.9F, 6.7F),
                PartPose.offset(-1.65F, -1.35F, 0.0F));
        harness.addOrReplaceChild(
                "upper_right_brass_buckle",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-0.45F, -0.45F, -0.18F, 0.9F, 0.9F, 0.36F),
                PartPose.offset(3.05F, -1.35F, -3.78F));
        harness.addOrReplaceChild(
                "upper_left_brass_buckle",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-0.45F, -0.45F, -0.18F, 0.9F, 0.9F, 0.36F),
                PartPose.offset(-1.05F, -1.35F, -3.78F));
        harness.addOrReplaceChild(
                "lower_right_brass_join",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-0.40F, -0.40F, -0.18F, 0.8F, 0.8F, 0.36F),
                PartPose.offset(4.60F, 1.35F, -3.78F));
        harness.addOrReplaceChild(
                "lower_left_brass_join",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-0.40F, -0.40F, -0.18F, 0.8F, 0.8F, 0.36F),
                PartPose.offset(-2.60F, 1.35F, -3.78F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static CubeListBuilder rightLeg(CubeDeformation deformation) {
        return CubeListBuilder.create().mirror().texOffs(0, 18)
                .addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation);
    }

    private static CubeListBuilder leftLeg(CubeDeformation deformation) {
        return CubeListBuilder.create().texOffs(0, 18)
                .addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation);
    }
}
