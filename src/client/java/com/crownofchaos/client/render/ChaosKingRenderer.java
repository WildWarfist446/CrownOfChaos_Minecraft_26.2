package com.crownofchaos.client.render;

import com.crownofchaos.CrownOfChaos;
import com.crownofchaos.entity.ChaosKing;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the Chaos King as a player-style figure in Warfist's skin
 * ({@code textures/entity/chaos_king.png}), with his armor layer so the Crown of Chaos he wears is
 * drawn by the crown's own renderer, like on a player.
 */
public final class ChaosKingRenderer extends HumanoidMobRenderer<ChaosKing, AvatarRenderState, PlayerModel> {
	private static final Identifier TEXTURE = CrownOfChaos.id("textures/entity/chaos_king.png");

	public ChaosKingRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		this.addLayer(new HumanoidArmorLayer<>(this,
				ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new PlayerModel(part, false)),
				context.getEquipmentRenderer()));
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return TEXTURE;
	}
}
