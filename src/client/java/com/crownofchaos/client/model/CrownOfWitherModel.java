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
 * The Crown of Wither's worn 3D model, converted from the Blockbench export in
 * {@code 3dmodels/Wither_Crown.java}: a crown band with four tall corner spikes and smaller
 * points around the rim.
 *
 * <p>The export's {@code head} part sits at y=24 (a player-head template); here it sits on
 * the humanoid head pivot so it copies the wearer's head transform.
 */
public final class CrownOfWitherModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(CrownOfChaos.id("crown_of_wither"), "main");
	public static final Identifier TEXTURE = CrownOfChaos.id("textures/entity/crown_of_wither.png");

	private CrownOfWitherModel() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);

		head.addOrReplaceChild("crown_of_wither", CubeListBuilder.create()
				.texOffs(0, 17).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
				// Tall corner spikes.
				.texOffs(37, 50).addBox(-5.0F, -6.0F, -5.0F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(44, 50).addBox(4.0F, -6.0F, -5.0F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(26, 50).addBox(4.0F, -6.0F, 4.0F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(55, 50).addBox(-5.0F, -6.0F, 4.0F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE)
				// Smaller points beside each spike.
				.texOffs(24, 44).addBox(-4.0F, -3.0F, -5.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(16, 47).addBox(-4.0F, -3.0F, 4.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(0, 37).addBox(3.0F, -3.0F, 4.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(0, 44).addBox(4.0F, -3.0F, -4.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(0, 48).addBox(3.0F, -3.0F, -5.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(0, 50).addBox(4.0F, -3.0F, 3.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(0, 54).addBox(-5.0F, -3.0F, 3.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(8, 59).addBox(-5.0F, -3.0F, -4.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, -8.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}
}
