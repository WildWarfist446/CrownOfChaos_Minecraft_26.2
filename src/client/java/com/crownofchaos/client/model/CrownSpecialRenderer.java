package com.crownofchaos.client.model;

import java.util.Map;
import java.util.function.Consumer;

import org.joml.Vector3fc;

import com.crownofchaos.CrownOfChaos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/**
 * Draws a crown's entity 3D model (the ones converted from Blockbench "Modded Entity" exports)
 * as its item, in the inventory, in hand, on the ground and in item frames, the way vanilla
 * draws tridents and mob heads. Item definitions use it as
 * {@code {"type": "minecraft:special", "model": {"type": "crownofchaos:crown", "crown": "<name>"}}};
 * the crowns made as Blockbench item models use their model JSON directly instead.
 */
public final class CrownSpecialRenderer implements NoDataSpecialModelRenderer {
	/** The crown models this renderer can draw, by the name item definitions use. */
	private static final Map<String, Crown> CROWNS = Map.of(
			"under_crown", new Crown(UnderCrownModel.LAYER, UnderCrownModel.TEXTURE),
			"crown_of_poseidon", new Crown(CrownOfPoseidonModel.LAYER, CrownOfPoseidonModel.TEXTURE),
			"crown_of_nether", new Crown(CrownOfNetherModel.LAYER, CrownOfNetherModel.TEXTURE),
			"crown_of_wither", new Crown(CrownOfWitherModel.LAYER, CrownOfWitherModel.TEXTURE));

	private final Model<Unit> model;
	private final Identifier texture;

	private CrownSpecialRenderer(Model<Unit> model, Identifier texture) {
		this.model = model;
		this.texture = texture;
	}

	/** Makes the {@code crownofchaos:crown} item model type available. */
	public static void register() {
		SpecialModelRenderers.ID_MAPPER.put(CrownOfChaos.id("crown"), Unbaked.MAP_CODEC);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
			int outlineColor) {
		collector.order(0).submitModel(this.model, Unit.INSTANCE, poseStack, RenderTypes.armorCutoutNoCull(this.texture), light,
				overlay, outlineColor, null);
		if (hasFoil) {
			collector.order(1).submitModel(this.model, Unit.INSTANCE, poseStack, RenderTypes.entityGlint(), light, overlay,
					outlineColor, null);
		}
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		this.model.root().getExtentsForGui(new PoseStack(), output);
	}

	private record Crown(ModelLayerLocation layer, Identifier texture) {
	}

	public record Unbaked(String crown) implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.STRING.validate(name -> CROWNS.containsKey(name)
						? DataResult.success(name)
						: DataResult.error(() -> "Unknown crown model: " + name))
						.fieldOf("crown").forGetter(Unbaked::crown))
				.apply(instance, Unbaked::new));

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public CrownSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
			Crown crown = CROWNS.get(this.crown);
			return new CrownSpecialRenderer(
					new Model.Simple(context.entityModelSet().bakeLayer(crown.layer()), RenderTypes::armorCutoutNoCull),
					crown.texture());
		}
	}
}
