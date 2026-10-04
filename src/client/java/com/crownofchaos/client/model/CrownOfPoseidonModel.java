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
 * The Crown of Poseidon's worn 3D model, converted from the Blockbench export in
 * {@code 3dmodels/Crown of Poseidon - Converted.java}: a crown band with a trident on the
 * front.
 *
 * <p>The export's {@code head} part sits at y=24 (a player-head template); here it sits on
 * the humanoid head pivot so it copies the wearer's head transform.
 */
public final class CrownOfPoseidonModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(CrownOfChaos.id("crown_of_poseidon"), "main");
	public static final Identifier TEXTURE = CrownOfChaos.id("textures/entity/crown_of_poseidon.png");

	private CrownOfPoseidonModel() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);

		head.addOrReplaceChild("crown_of_poseidon", CubeListBuilder.create()
				.texOffs(0, 16).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
				// Trident shaft and prongs on the front of the crown.
				.texOffs(34, 0).addBox(-1.0F, -4.0F, -6.0F, 2.0F, 7.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(0, 32).addBox(-2.0F, -2.0F, -6.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(6, 32).addBox(1.0F, -2.0F, -6.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 1).addBox(-3.0F, -3.0F, -6.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE)
				.texOffs(1, 5).addBox(2.0F, -3.0F, -6.0F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, -8.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}
}
