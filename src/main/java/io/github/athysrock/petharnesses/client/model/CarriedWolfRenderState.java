package io.github.athysrock.petharnesses.client.model;

import net.minecraft.client.renderer.entity.state.WolfRenderState;

public final class CarriedWolfRenderState extends WolfRenderState {
    public float rightFrontAngle = 25.0F * (float) Math.PI / 72.0F;
    public float leftFrontAngle = (float) Math.PI / 8.0F;
    public float rightHindAngle = (float) Math.PI / 3.0F;
    public float leftHindAngle = (float) Math.PI / 6.0F;
}
