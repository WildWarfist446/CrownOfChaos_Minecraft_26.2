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
 * The Crown of Nether's worn 3D model, converted from the Blockbench export in
 * {@code 3dmodels/Nether Crown.java}: a crown band with gold points around the top rim.
 *
 * <p>The export's {@code head} part sits at y=24 (a player-head template); here it sits on
 * the humanoid head pivot so it copies the wearer's head transform, and the template's own
 * head cubes are left out.
 */
public final class CrownOfNetherModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(CrownOfChaos.id("crown_of_nether"), "main");
	public static final Identifier TEXTURE = CrownOfChaos.id("textures/entity/crown_of_nether.png");

	private CrownOfNetherModel() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);

		head.addOrReplaceChild("crown_of_nether", CubeListBuilder.create()
				.texOffs(0, 16).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
				// Points around the top rim: front and back.
				.texOffs(1, 1).addBox(-1.0F, -2.0F, 4.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(-1.0F, -2.0F, -5.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(-5.0F, -2.0F, -5.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(3.0F, -2.0F, -5.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(3.0F, -2.0F, 4.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(-5.0F, -2.0F, 4.0F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				// Corners and sides.
				.texOffs(1, 1).addBox(-5.0F, -2.0F, 3.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(-5.0F, -2.0F, -4.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(4.0F, -2.0F, -4.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(4.0F, -2.0F, 3.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(0, 0).addBox(4.0F, -2.0F, -1.0F, 1.0F, 1.0F, 2.0F, CubeDeformation.NONE)
				.texOffs(0, 0).addBox(-5.0F, -2.0F, -1.0F, 1.0F, 1.0F, 2.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, -8.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}
}
