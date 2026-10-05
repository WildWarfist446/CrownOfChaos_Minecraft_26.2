package com.crownofchaos.block;

import com.crownofchaos.menu.CommanderTableMenu;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Commander's Table: opens the menu for making army units from a crown (see
 * {@link CommanderTableMenu}). Like a crafting table, it keeps no items itself. It uses
 * Warfist's 3D model (a table on a single stem, with a map and pieces on top) and turns to face
 * whoever places it.
 */
public class CommanderTableBlock extends Block {
	public static final MapCodec<CommanderTableBlock> CODEC = simpleCodec(CommanderTableBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final Component TITLE = Component.translatable("container.crownofchaos.commander_table");

	/**
	 * The stem and tabletop, for facing north or south (the model's long side runs north to
	 * south). The tabletop overhangs the block by 2 pixels at each end; the shape stops at the
	 * block's edge.
	 */
	private static final VoxelShape SHAPE_NORTH_SOUTH = Shapes.or(
			Block.box(7.0, 0.0, 7.0, 9.0, 7.0, 9.0),
			Block.box(1.0, 7.0, 0.0, 15.0, 9.0, 16.0));
	/** The same, turned a quarter for facing east or west. */
	private static final VoxelShape SHAPE_EAST_WEST = Shapes.or(
			Block.box(7.0, 0.0, 7.0, 9.0, 7.0, 9.0),
			Block.box(0.0, 7.0, 1.0, 16.0, 9.0, 15.0));

	public CommanderTableBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<? extends CommanderTableBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NORTH_SOUTH : SHAPE_EAST_WEST;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(state.getMenuProvider(level, pos));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider(
				(containerId, inventory, player) -> new CommanderTableMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
	}
}
