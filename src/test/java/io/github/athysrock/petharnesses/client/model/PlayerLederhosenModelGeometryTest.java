package io.github.athysrock.petharnesses.client.model;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class PlayerLederhosenModelGeometryTest {
    private static final float EPSILON = 1.0E-4F;
    private static final float MIN_OUTER_LAYER_CLEARANCE = 0.10F;

    @Test
    void constructorAcceptsTheBakedLayerUsedByLiveAvatarRenderer() {
        assertDoesNotThrow(() -> new PlayerLederhosenModel(
                PlayerLederhosenModel.createBodyLayer().bakeRoot()));
    }

    @Test
    void bakesTorsoOnlySuspendersAndIndependentShortLegCuffs() {
        ModelPart root = PlayerLederhosenModel.createBodyLayer().bakeRoot();
        ModelPart body = root.getChild("body");
        ModelPart rightLeg = root.getChild("right_leg");
        ModelPart leftLeg = root.getChild("left_leg");

        assertTrue(body.hasChild("raised_waistband"));
        assertTrue(body.hasChild("front_fall_bib"));
        assertTrue(body.hasChild("right_suspender"));
        assertTrue(body.hasChild("left_suspender"));
        assertTrue(body.hasChild("chest_bridge"));
        assertTrue(body.hasChild("brass_hardware"));
        assertTrue(body.hasChild("cream_piping"));
        assertTrue(rightLeg.hasChild("right_short_cuff"));
        assertTrue(leftLeg.hasChild("left_short_cuff"));

        assertFalse(root.getChild("right_arm").hasChild("right_suspender"));
        assertFalse(root.getChild("left_arm").hasChild("left_suspender"));
    }

    @Test
    void harnessCubesHaveNoCoincidentCoplanarFaces() {
        assertNoCoincidentFaces(new PlayerLederhosenModel(
                PlayerLederhosenModel.createBodyLayer().bakeRoot()), "baked zero pose");

        for (boolean crouching : new boolean[] {false, true}) {
            PlayerLederhosenModel model = new PlayerLederhosenModel(
                    PlayerLederhosenModel.createBodyLayer().bakeRoot());
            AvatarRenderState state = standingState();
            state.isCrouching = crouching;
            model.setupAnim(state);
            assertNoCoincidentFaces(model, crouching ? "crouching" : "standing");
        }
    }

    private static void assertNoCoincidentFaces(PlayerLederhosenModel model, String poseName) {
        List<NamedBounds> cubes = transformedBounds(model.root());
        for (int first = 0; first < cubes.size(); first++) {
            for (int second = first + 1; second < cubes.size(); second++) {
                NamedBounds a = cubes.get(first);
                NamedBounds b = cubes.get(second);
                assertFalse(hasCoincidentCoplanarFace(a.bounds, b.bounds),
                        () -> poseName + " coincident harness faces: " + a.name + " " + a.bounds
                                + " and " + b.name + " " + b.bounds);
            }
        }
    }

    @Test
    void standardAndSlimHarnessClearBaseSkinAndOuterLayersWhileStandingAndCrouching() {
        for (boolean slim : new boolean[] {false, true}) {
            assertPlayerLayerClearance(slim, false);
            assertPlayerLayerClearance(slim, true);
        }
    }

    @Test
    void shoulderAndChestStrapsRemainDistinctAndPreserveTheUnderNeckSilhouette() {
        ModelPart body = PlayerLederhosenModel.createBodyLayer().bakeRoot().getChild("body");
        assertFalse(body.hasChild("shoulder_returns"),
                "coplanar torso-top return cubes must stay consolidated into the suspenders");

        CubeBounds right = onlyBounds(body.getChild("right_suspender"));
        CubeBounds left = onlyBounds(body.getChild("left_suspender"));
        CubeBounds bridge = onlyBounds(body.getChild("chest_bridge"));

        assertTrue(right.minY <= -0.30F && left.minY <= -0.30F,
                "consolidated straps must preserve the visible under-neck/shoulder silhouette");
        assertFalse(overlaps(right, bridge));
        assertFalse(overlaps(left, bridge));
    }

    private static void assertPlayerLayerClearance(boolean slim, boolean crouching) {
        AvatarRenderState state = standingState();
        state.isCrouching = crouching;

        PlayerLederhosenModel harness = new PlayerLederhosenModel(
                PlayerLederhosenModel.createBodyLayer().bakeRoot());
        ModelPart playerRoot = LayerDefinition.create(
                PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
        PlayerModel player = new PlayerModel(playerRoot, slim);
        harness.setupAnim(state);
        player.setupAnim(state);

        assertPartClearance(
                harness.root(), harness.body, player.root(), player.body,
                List.of("raised_waistband"), List.of(
                        "waistband_highlight", "front_fall_bib", "right_suspender", "left_suspender",
                        "chest_bridge", "brass_hardware", "cream_piping"),
                player.body.getChild("jacket"), "body", slim, crouching);
        assertPartClearance(
                harness.root(), harness.rightLeg, player.root(), player.rightLeg,
                List.of("right_short_cuff"), List.of("right_short_cuff"),
                player.rightLeg.getChild("right_pants"), "right leg", slim, crouching);
        assertPartClearance(
                harness.root(), harness.leftLeg, player.root(), player.leftLeg,
                List.of("left_short_cuff"), List.of("left_short_cuff"),
                player.leftLeg.getChild("left_pants"), "left leg", slim, crouching);
    }

    private static void assertPartClearance(
            ModelPart harnessRoot,
            ModelPart harnessParent,
            ModelPart playerRoot,
            ModelPart playerParent,
            List<String> radialShells,
            List<String> frontParts,
            ModelPart outerPart,
            String label,
            boolean slim,
            boolean crouching) {
        Matrix4f harnessPose = partPose(harnessRoot, harnessParent);
        Matrix4f playerPose = partPose(playerRoot, playerParent);
        CubeBounds base = visualBounds(ownCubes(playerParent).getFirst());
        CubeBounds outer = visualBounds(ownCubes(outerPart).getFirst());
        String context = label + " slim=" + slim + " crouching=" + crouching;

        for (String name : radialShells) {
            for (ModelPart.Cube cube : ownCubes(harnessParent.getChild(name))) {
                CubeBounds bounds = visualBounds(cube);
                if (bounds.maxZ - bounds.minZ <= 1.0F) continue;
                boolean checkNegativeX = !label.equals("left leg");
                boolean checkPositiveX = !label.equals("right leg");
                assertRadialClearance(bounds, harnessPose, outer, playerPose,
                        MIN_OUTER_LAYER_CLEARANCE, checkNegativeX, checkPositiveX,
                        context + " outer " + name);
                assertRadialClearance(bounds, harnessPose, base, playerPose,
                        MIN_OUTER_LAYER_CLEARANCE + 0.25F, checkNegativeX, checkPositiveX,
                        context + " base " + name);
            }
        }
        for (String name : frontParts) {
            for (ModelPart.Cube cube : ownCubes(harnessParent.getChild(name))) {
                CubeBounds bounds = visualBounds(cube);
                if (bounds.maxZ - bounds.minZ > 1.0F) continue;
                assertFrontClearance(bounds, harnessPose, outer, playerPose,
                        MIN_OUTER_LAYER_CLEARANCE, context + " outer " + name);
                assertFrontClearance(bounds, harnessPose, base, playerPose,
                        MIN_OUTER_LAYER_CLEARANCE + 0.25F, context + " base " + name);
            }
        }
    }

    private static void assertRadialClearance(
            CubeBounds harness,
            Matrix4f harnessPose,
            CubeBounds skin,
            Matrix4f skinPose,
            float minimum,
            boolean checkNegativeX,
            boolean checkPositiveX,
            String context) {
        if (checkNegativeX) {
            assertPlaneClearance(harness.minX, skin.minX, new Vector3f(-1, 0, 0), harnessPose, skinPose, minimum, context + " -X");
        }
        if (checkPositiveX) {
            assertPlaneClearance(harness.maxX, skin.maxX, new Vector3f(1, 0, 0), harnessPose, skinPose, minimum, context + " +X");
        }
        assertPlaneClearance(harness.minZ, skin.minZ, new Vector3f(0, 0, -1), harnessPose, skinPose, minimum, context + " front");
        assertPlaneClearance(harness.maxZ, skin.maxZ, new Vector3f(0, 0, 1), harnessPose, skinPose, minimum, context + " back");
    }

    private static void assertFrontClearance(
            CubeBounds harness,
            Matrix4f harnessPose,
            CubeBounds skin,
            Matrix4f skinPose,
            float minimum,
            String context) {
        Vector3f normal = new Vector3f(0, 0, -1);
        Vector3f harnessPoint = transform(harnessPose, 0, 0, harness.maxZ);
        Vector3f skinPoint = transform(skinPose, 0, 0, skin.minZ);
        harnessPose.transformDirection(normal).normalize();
        float distance = harnessPoint.sub(skinPoint).dot(normal);
        assertTrue(distance + EPSILON >= minimum,
                () -> context + " clearance=" + distance + " required=" + minimum);
    }

    private static void assertPlaneClearance(
            float harnessCoordinate,
            float skinCoordinate,
            Vector3f localNormal,
            Matrix4f harnessPose,
            Matrix4f skinPose,
            float minimum,
            String context) {
        Vector3f harnessPoint = transformAlongAxis(harnessPose, harnessCoordinate, localNormal);
        Vector3f skinPoint = transformAlongAxis(skinPose, skinCoordinate, localNormal);
        Vector3f normal = harnessPose.transformDirection(new Vector3f(localNormal)).normalize();
        float distance = harnessPoint.sub(skinPoint).dot(normal);
        assertTrue(distance + EPSILON >= minimum,
                () -> context + " clearance=" + distance + " required=" + minimum);
    }

    private static Vector3f transformAlongAxis(Matrix4f pose, float coordinate, Vector3f axis) {
        return transform(pose, coordinate * Math.abs(axis.x), coordinate * Math.abs(axis.y), coordinate * Math.abs(axis.z));
    }

    private static Vector3f transform(Matrix4f pose, float x, float y, float z) {
        return pose.transformPosition(x / 16.0F, y / 16.0F, z / 16.0F, new Vector3f()).mul(16.0F);
    }

    private static Matrix4f partPose(ModelPart root, ModelPart part) {
        PoseStack stack = new PoseStack();
        root.translateAndRotate(stack);
        part.translateAndRotate(stack);
        return new Matrix4f(stack.last().pose());
    }

    private static List<ModelPart.Cube> ownCubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (path.isEmpty()) cubes.add(cube);
        });
        return cubes;
    }

    private static CubeBounds visualBounds(ModelPart.Cube cube) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                minX = Math.min(minX, vertex.x());
                minY = Math.min(minY, vertex.y());
                minZ = Math.min(minZ, vertex.z());
                maxX = Math.max(maxX, vertex.x());
                maxY = Math.max(maxY, vertex.y());
                maxZ = Math.max(maxZ, vertex.z());
            }
        }
        return new CubeBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static CubeBounds onlyBounds(ModelPart part) {
        List<NamedBounds> cubes = transformedBounds(part);
        assertEquals(1, cubes.size());
        return cubes.getFirst().bounds;
    }

    private static List<NamedBounds> transformedBounds(ModelPart part) {
        List<NamedBounds> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            CubeBounds visual = visualBounds(cube);
            float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (float x : new float[] {visual.minX, visual.maxX}) {
                for (float y : new float[] {visual.minY, visual.maxY}) {
                    for (float z : new float[] {visual.minZ, visual.maxZ}) {
                        Vector3f point = pose.pose().transformPosition(
                                x / 16.0F, y / 16.0F, z / 16.0F, new Vector3f()).mul(16.0F);
                        bounds[0] = Math.min(bounds[0], point.x); bounds[1] = Math.min(bounds[1], point.y);
                        bounds[2] = Math.min(bounds[2], point.z); bounds[3] = Math.max(bounds[3], point.x);
                        bounds[4] = Math.max(bounds[4], point.y); bounds[5] = Math.max(bounds[5], point.z);
                    }
                }
            }
            cubes.add(new NamedBounds(path + "[" + index + "]",
                    new CubeBounds(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5])));
        });
        return cubes;
    }

    private static boolean hasCoincidentCoplanarFace(CubeBounds a, CubeBounds b) {
        return coincident(a.minX, b.minX) && overlaps(a.minY, a.maxY, b.minY, b.maxY) && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)
                || coincident(a.minX, b.maxX) && overlaps(a.minY, a.maxY, b.minY, b.maxY) && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)
                || coincident(a.maxX, b.minX) && overlaps(a.minY, a.maxY, b.minY, b.maxY) && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)
                || coincident(a.maxX, b.maxX) && overlaps(a.minY, a.maxY, b.minY, b.maxY) && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)
                || coincident(a.minY, b.minY) && overlaps(a.minX, a.maxX, b.minX, b.maxX) && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)
                || coincident(a.minY, b.maxY) && overlaps(a.minX, a.maxX, b.minX, b.maxX) && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)
                || coincident(a.maxY, b.minY) && overlaps(a.minX, a.maxX, b.minX, b.maxX) && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)
                || coincident(a.maxY, b.maxY) && overlaps(a.minX, a.maxX, b.minX, b.maxX) && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ)
                || coincident(a.minZ, b.minZ) && overlaps(a.minX, a.maxX, b.minX, b.maxX) && overlaps(a.minY, a.maxY, b.minY, b.maxY)
                || coincident(a.minZ, b.maxZ) && overlaps(a.minX, a.maxX, b.minX, b.maxX) && overlaps(a.minY, a.maxY, b.minY, b.maxY)
                || coincident(a.maxZ, b.minZ) && overlaps(a.minX, a.maxX, b.minX, b.maxX) && overlaps(a.minY, a.maxY, b.minY, b.maxY)
                || coincident(a.maxZ, b.maxZ) && overlaps(a.minX, a.maxX, b.minX, b.maxX) && overlaps(a.minY, a.maxY, b.minY, b.maxY);
    }

    private static boolean coincident(float a, float b) {
        return Math.abs(a - b) <= EPSILON;
    }

    private static boolean overlaps(float aMin, float aMax, float bMin, float bMax) {
        return aMin < bMax - EPSILON && aMax > bMin + EPSILON;
    }

    private static boolean overlaps(CubeBounds a, CubeBounds b) {
        return overlaps(a.minX, a.maxX, b.minX, b.maxX)
                && overlaps(a.minY, a.maxY, b.minY, b.maxY)
                && overlaps(a.minZ, a.maxZ, b.minZ, b.maxZ);
    }

    private static AvatarRenderState standingState() {
        AvatarRenderState state = new AvatarRenderState();
        state.speedValue = 1.0F;
        return state;
    }

    private record NamedBounds(String name, CubeBounds bounds) {}
    private record CubeBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}
}
