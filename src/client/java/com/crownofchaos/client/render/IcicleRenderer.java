package com.crownofchaos.client.render;

import com.crownofchaos.entity.IcicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Draws a Crown of Winter icicle as its block (Packed Ice) squeezed into a thin shard the size
 * of the icicle's hitbox, using the vanilla falling block renderer.
 */
public class IcicleRenderer extends FallingBlockRenderer {
	public IcicleRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = IcicleEntity.WIDTH / 2.0F;
	}

	@Override
	public void submit(FallingBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.scale(IcicleEntity.WIDTH, IcicleEntity.HEIGHT, IcicleEntity.WIDTH);
		super.submit(state, poseStack, submitNodeCollector, camera);
		poseStack.popPose();
	}
}
