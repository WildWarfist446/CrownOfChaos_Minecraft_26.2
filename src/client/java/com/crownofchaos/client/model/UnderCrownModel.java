package com.crownofchaos.client.model;

import com.crownofchaos.CrownOfChaos;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * The Under Crown's worn 3D model, converted from the Blockbench export in
 * {@code 3dmodels/Undercrown}.
 *
 * <p>The crown is a child of a part named {@code head} so it copies the wearer's head
 * position and rotation. The export was built on a player-head template sitting at y=24, so
 * every part is shifted up by 24 to sit on the humanoid head pivot instead, and the
 * template's own head cubes are left out.
 */
public final class UnderCrownModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(CrownOfChaos.id("under_crown"), "main");
	public static final Identifier TEXTURE = CrownOfChaos.id("textures/entity/under_crown.png");

	private UnderCrownModel() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);

		PartDefinition undercrown = head.addOrReplaceChild("undercrown", CubeListBuilder.create()
				.texOffs(13, 18).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F)),
				PartPose.offset(0.0F, -8.0F, 0.0F));

		undercrown.addOrReplaceChild("left_horn", horn(), PartPose.offset(6.0F, 8.0F, -2.0F));
		undercrown.addOrReplaceChild("right_horn", horn(), PartPose.offset(-5.0F, 8.0F, -2.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	private static CubeListBuilder horn() {
		return CubeListBuilder.create()
				.texOffs(6, 33).addBox(-1.0F, -8.0F, 0.0F, 1.0F, 3.0F, 3.0F, CubeDeformation.NONE)
				.texOffs(0, 0).addBox(-1.0F, -6.0F, 2.0F, 1.0F, 3.0F, 2.0F, CubeDeformation.NONE)
				.texOffs(0, 0).addBox(-1.0F, -3.0F, 1.0F, 1.0F, 2.0F, 2.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(-1.0F, -2.0F, 0.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE);
	}
}
