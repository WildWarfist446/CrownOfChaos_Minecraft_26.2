package com.crownofchaos.client.model;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a crown's custom 3D model in place of the vanilla helmet. The model's {@code head}
 * part copies the wearer's head transform, so the crown turns and crouches with them.
 */
public final class CrownArmorRenderer implements ArmorRenderer {
	private final Model<Unit> model;
	private final Identifier texture;

	public CrownArmorRenderer(ModelPart root, Identifier texture) {
		this.model = new Model.Simple(root, RenderTypes::armorCutoutNoCull);
		this.texture = texture;
	}

	@Override
	public void render(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, HumanoidRenderState state,
			EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> contextModel) {
		if (slot != EquipmentSlot.HEAD || FirstPersonCrowns.hide(state, contextModel)) {
			return;
		}

		ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, Unit.INSTANCE, false, collector.order(0),
				poseStack, RenderTypes.armorCutoutNoCull(texture), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);

		// Enchantment shimmer, like vanilla armor.
		if (stack.hasFoil()) {
			ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, Unit.INSTANCE, false, collector.order(1),
					poseStack, RenderTypes.armorEntityGlint(), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		}
	}
}
