package de.ellpeck.actuallyadditions.fabric.power;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class PowerBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public final int tier;
    public PowerBlock(int tier, Properties properties) {
        super(properties); this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, tier < 0 ? Direction.NORTH : Direction.UP).setValue(ACTIVE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, tier < 0 ? context.getHorizontalDirection().getOpposite() : context.getClickedFace());
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return tier < 0 ? Shapes.block() : Block.box(1, 1, 1, 15, 15, 15);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return tier < 0 ? new GeneratorEntity(pos, state) : new RelayEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type == PowerMod.GENERATOR_ENTITY) return (w, p, s, e) -> ((GeneratorEntity) e).tick();
        if (type == PowerMod.RELAY_ENTITY) return (w, p, s, e) -> ((RelayEntity) e).tick();
        return null;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof LaserWrench) return InteractionResult.PASS;
        if (tier >= 0 && stack.is(PowerMod.ITEMS.get("laser_upgrade_range"))) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RelayEntity relay) {
                if (!relay.rangeUpgrade) {
                    relay.rangeUpgrade = true; relay.changed();
                    if (!player.isCreative()) stack.shrink(1);
                    player.sendOverlayMessage(Component.literal("Relay range upgraded to 35 blocks."));
                } else player.sendOverlayMessage(Component.literal("This relay already has a range upgrade."));
            }
            return InteractionResult.SUCCESS;
        }
        return interact(level, pos, player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return interact(level, pos, player); }
    private InteractionResult interact(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof GeneratorEntity generator) player.openMenu(generator);
            else if (level.getBlockEntity(pos) instanceof RelayEntity relay) {
                if (player.isShiftKeyDown()) { relay.mode = RelayEntity.Mode.values()[(relay.mode.ordinal() + 1) % 3]; relay.changed(); }
                player.sendOverlayMessage(Component.literal(relay.status()));
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side) {
        if (level.getBlockEntity(pos) instanceof GeneratorEntity generator) return generator.energy() * 15 / GeneratorEntity.CAPACITY;
        return 0;
    }
}
