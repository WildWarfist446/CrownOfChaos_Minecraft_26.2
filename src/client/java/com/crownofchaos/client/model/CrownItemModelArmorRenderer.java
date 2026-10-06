package com.crownofchaos.client.model;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Draws a crown on the head from a Blockbench "Java Item" model (an item model JSON) instead of
 * an entity model: a stand-in stack that uses the item model is drawn the way vanilla draws a
 * non-armor item on a head. The model's {@code head} display transform in its JSON is where to
 * line it up.
 */
public final class CrownItemModelArmorRenderer implements ArmorRenderer {
	private final Identifier itemModel;
	/** Made on first use: stacks can't be created while the renderers are being set up. */
	private ItemStack wornStack;
	private final ItemStackRenderState renderState = new ItemStackRenderState();

	/** @param itemModel the item model (an {@code items/<name>.json} definition) to draw */
	public CrownItemModelArmorRenderer(Identifier itemModel) {
		this.itemModel = itemModel;
	}

	@Override
	public void render(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, HumanoidRenderState state,
			EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> contextModel) {
		if (slot != EquipmentSlot.HEAD) {
			return;
		}
		if (this.wornStack == null) {
			this.wornStack = new ItemStack(Items.STICK);
			this.wornStack.set(DataComponents.ITEM_MODEL, this.itemModel);
			this.wornStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		Minecraft client = Minecraft.getInstance();
		client.getItemModelResolver().updateForTopItem(this.renderState, this.wornStack, ItemDisplayContext.HEAD, client.level, null, 0);
		poseStack.pushPose();
		contextModel.root().translateAndRotate(poseStack);
		contextModel.translateToHead(poseStack);
		CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
		this.renderState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
