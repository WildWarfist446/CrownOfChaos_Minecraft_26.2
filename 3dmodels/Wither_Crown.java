// Made with Blockbench 5.2.1
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class Wither_Crown<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "wither_crown"), "main");
	private final ModelPart head;
	private final ModelPart Wither Crown;

	public Wither_Crown(ModelPart root) {
		this.head = root.getChild("head");
		this.Wither Crown = this.head.getChild("Wither Crown");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Wither Crown = head.addOrReplaceChild("Wither Crown", CubeListBuilder.create().texOffs(0, 17).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
		.texOffs(37, 50).addBox(-5.0F, -6.0F, -5.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(44, 50).addBox(4.0F, -6.0F, -5.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(26, 50).addBox(4.0F, -6.0F, 4.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(55, 50).addBox(-5.0F, -6.0F, 4.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(24, 44).addBox(-4.0F, -3.0F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(16, 47).addBox(-4.0F, -3.0F, 4.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 37).addBox(3.0F, -3.0F, 4.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 44).addBox(4.0F, -3.0F, -4.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 48).addBox(3.0F, -3.0F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 50).addBox(4.0F, -3.0F, 3.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 54).addBox(-5.0F, -3.0F, 3.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(8, 59).addBox(-5.0F, -3.0F, -4.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}