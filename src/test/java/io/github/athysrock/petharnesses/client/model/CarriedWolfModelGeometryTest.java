package io.github.athysrock.petharnesses.client.model;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;

final class CarriedWolfModelGeometryTest {
    private static final float EPSILON = 1.0e-5F;

    @Test
    void bakedNativeModelMatchesFullScaleMountAndAuthoredPose() {
        ModelPart root = CarriedWolfModel.createBodyLayer().bakeRoot();
        ModelPart mount = root.getChild("front_mount_root");
        assertPose(mount, 0.0F, 8.9F, -6.9F, -(float) Math.PI / 2.0F);
        ModelPart scale = mount.getChild("runtime_scale_root");
        assertEquals(1.0F, scale.xScale, EPSILON);
        assertEquals(1.0F, scale.yScale, EPSILON);
        assertEquals(1.0F, scale.zScale, EPSILON);
        ModelPart carry = scale.getChild("carry_pose");
        assertPose(carry, 0.0F, -14.0F, 0.0F, 0.0F);
        ModelPart source = carry.getChild("source_root");
        ModelPart harness = carry.getChild("harness_root");
        assertTrue(harness.hasChild("upper_chest_strap"));
        assertTrue(harness.hasChild("lower_stomach_strap"));
        assertTrue(harness.hasChild("center_connector"));
        assertTrue(harness.hasChild("right_upper_side_wrap"));
        assertTrue(harness.hasChild("left_upper_side_wrap"));
        assertTrue(harness.hasChild("upper_right_brass_buckle"));
        assertTrue(harness.hasChild("upper_left_brass_buckle"));
        assertTrue(harness.hasChild("lower_right_brass_join"));
        assertTrue(harness.hasChild("lower_left_brass_join"));
        assertFalse(harness.hasChild("right_shoulder_strap"));
        assertFalse(harness.hasChild("left_shoulder_strap"));
        assertFalse(harness.hasChild("center_brass_junction"));
        assertFalse(harness.hasChild("sternum_strap"));
        assertFalse(source.hasChild("harness_root"));

        assertPose(source.getChild("head"), -1.0F, 13.5F, -7.0F, 11.0F * (float) Math.PI / 24.0F);
        assertPose(source.getChild("body"), 0.0F, 14.0F, 2.0F, (float) Math.PI / 2.0F);
        assertPose(source.getChild("upper_body"), -1.0F, 14.0F, -3.0F, (float) Math.PI / 2.0F);
        assertPose(source.getChild("right_hind_leg"), -2.5F, 16.0F, 5.0F, (float) Math.PI / 3.0F);
        assertPose(source.getChild("left_hind_leg"), 0.5F, 16.0F, 5.0F, (float) Math.PI / 6.0F);
        assertPose(source.getChild("right_front_leg"), -2.5F, 16.0F, -4.0F, 25.0F * (float) Math.PI / 72.0F);
        assertPose(source.getChild("left_front_leg"), 0.5F, 16.0F, -4.0F, (float) Math.PI / 8.0F);
        assertPose(source.getChild("tail"), -1.0F, 12.0F, 7.0F, (float) Math.PI / 5.0F);
        assertTrue(source.getChild("head").hasChild("real_head"));
        assertTrue(source.getChild("tail").hasChild("real_tail"));
    }

    @Test
    void squareIHarnessUsesTwoHorizontalBarsAndOneCenteredVerticalConnector() {
        ModelPart carry = CarriedWolfModel.createBodyLayer().bakeRoot()
                .getChild("front_mount_root").getChild("runtime_scale_root").getChild("carry_pose");
        ModelPart source = carry.getChild("source_root");
        ModelPart harness = carry.getChild("harness_root");
        ModelPart upperBody = source.getChild("upper_body");

        assertPose(harness, upperBody.x, upperBody.y, upperBody.z, upperBody.xRot);

        ModelPart upperPart = harness.getChild("upper_chest_strap");
        ModelPart lowerPart = harness.getChild("lower_stomach_strap");
        ModelPart connectorPart = harness.getChild("center_connector");
        CubeBounds upper = transformedBounds(upperPart);
        CubeBounds lower = transformedBounds(lowerPart);
        CubeBounds connector = transformedBounds(connectorPart);

        assertEquals(0.0F, upperPart.zRot, EPSILON, "upper bar must not slope into a Y");
        assertEquals(0.0F, lowerPart.zRot, EPSILON, "lower bar must stay horizontal");
        assertEquals(0.0F, connectorPart.zRot, EPSILON, "connector must stay vertical");
        assertEquals(5.0F, width(upper), EPSILON,
                "upper chest strap must use the former short lower-bar span");
        assertEquals(8.0F, width(lower), EPSILON,
                "lower stomach strap must use the former long upper-bar span");
        assertTrue(width(lower) > width(upper),
                "the lower stomach bar must be visibly wider than the upper chest bar");
        assertTrue(height(upper) <= 1.0F && height(lower) <= 1.0F,
                "both crossbars must remain compact and horizontal");
        assertTrue(height(connector) >= 2.0F && width(connector) <= 1.0F,
                "one narrow centered strap must connect the bars");
        assertEquals(1.0F, centerX(upper), 0.05F,
                "upper bar must be centered on the upper-body cube in harness-local space");
        assertEquals(centerX(upper), centerX(lower), 0.05F);
        assertEquals(centerX(upper), centerX(connector), 0.05F);
        assertTrue(upper.maxY + EPSILON >= connector.minY
                        && connector.maxY + EPSILON >= lower.minY,
                "the connector must overlap both bars without gaps");
        assertTrue(upper.maxY < lower.minY,
                "the two leather bars must remain visually distinct");
        assertTrue(upper.maxZ <= -3.20F && lower.maxZ <= -3.20F,
                "both bars must clear the upper-body front face at z=-3");
        assertTrue(upper.minY >= -2.2F && lower.maxY <= 2.2F,
                "the compact I must stay on the square upper body below the muzzle");

        assertPose(upperPart, 1.0F, -1.35F, -3.45F, 0.0F);
        assertPose(lowerPart, 1.0F, 1.35F, -3.45F, 0.0F);
        assertPose(connectorPart, 1.0F, -0.90F, -3.45F, 0.0F);
        assertPose(harness.getChild("upper_right_brass_buckle"), 3.05F, -1.35F, -3.78F, 0.0F);
        assertPose(harness.getChild("upper_left_brass_buckle"), -1.05F, -1.35F, -3.78F, 0.0F);
        assertPose(harness.getChild("lower_right_brass_join"), 4.60F, 1.35F, -3.78F, 0.0F);
        assertPose(harness.getChild("lower_left_brass_join"), -2.60F, 1.35F, -3.78F, 0.0F);

        CubeBounds rightWrap = transformedBounds(harness.getChild("right_upper_side_wrap"));
        CubeBounds leftWrap = transformedBounds(harness.getChild("left_upper_side_wrap"));
        assertPose(harness.getChild("right_upper_side_wrap"), 3.65F, -1.35F, 0.0F, 0.0F);
        assertPose(harness.getChild("left_upper_side_wrap"), -1.65F, -1.35F, 0.0F, 0.0F);
        assertTrue(width(rightWrap) <= 0.40F && width(leftWrap) <= 0.40F,
                "upper chest bar ends must continue as narrow side wraps");
        assertTrue(depth(rightWrap) >= 5.0F && depth(leftWrap) >= 5.0F,
                "side wraps must travel around the blocky torso rather than forming another front belt");
    }

    private static float width(CubeBounds bounds) {
        return bounds.maxX - bounds.minX;
    }

    private static float height(CubeBounds bounds) {
        return bounds.maxY - bounds.minY;
    }

    private static float depth(CubeBounds bounds) {
        return bounds.maxZ - bounds.minZ;
    }

    private static float centerX(CubeBounds bounds) {
        return (bounds.minX + bounds.maxX) / 2.0F;
    }

    private static CubeBounds transformedBounds(ModelPart part) {
        List<CubeBounds> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (float x : new float[] {cube.minX, cube.maxX}) {
                for (float y : new float[] {cube.minY, cube.maxY}) {
                    for (float z : new float[] {cube.minZ, cube.maxZ}) {
                        Vector3f point = pose.pose().transformPosition(
                                x / 16.0F, y / 16.0F, z / 16.0F, new Vector3f()).mul(16.0F);
                        bounds[0] = Math.min(bounds[0], point.x); bounds[1] = Math.min(bounds[1], point.y);
                        bounds[2] = Math.min(bounds[2], point.z); bounds[3] = Math.max(bounds[3], point.x);
                        bounds[4] = Math.max(bounds[4], point.y); bounds[5] = Math.max(bounds[5], point.z);
                    }
                }
            }
            cubes.add(new CubeBounds(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]));
        });
        assertEquals(1, cubes.size());
        return cubes.getFirst();
    }

    private record CubeBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}

    private static void assertPose(ModelPart part, float x, float y, float z, float xRot) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(xRot, part.xRot, EPSILON);
    }
}
