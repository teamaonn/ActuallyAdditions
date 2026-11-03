package de.ellpeck.actuallyadditions.fabric.content;

import de.ellpeck.actuallyadditions.fabric.power.PowerBlock;
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
import net.minecraft.world.phys.BlockHitResult;

public final class WorkBlock extends Block implements EntityBlock {
    public final WorkKind kind;
    public WorkBlock(WorkKind kind,Properties properties){super(properties);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(PowerBlock.FACING,Direction.NORTH).setValue(PowerBlock.ACTIVE,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PowerBlock.FACING,PowerBlock.ACTIVE);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(PowerBlock.FACING,(kind==WorkKind.FLUID_PLACER||kind==WorkKind.FLUID_COLLECTOR||kind==WorkKind.RECONSTRUCTOR||kind==WorkKind.BREAKER||kind==WorkKind.PLACER||kind==WorkKind.DROPPER||kind==WorkKind.LONG_BREAKER)?c.getNearestLookingDirection().getOpposite():c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new WorkEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide()&&type==Content.ENTITY?(w,p,s,e)->((WorkEntity)e).tick():null;}
    @Override protected boolean isSignalSource(BlockState s){return kind==WorkKind.PHANTOM_REDSTONE;}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return kind==WorkKind.PHANTOM_REDSTONE&&l.getBlockEntity(p) instanceof WorkEntity e?e.weakSignal[d.ordinal()]:0;}
    @Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return kind==WorkKind.PHANTOM_REDSTONE&&l.getBlockEntity(p) instanceof WorkEntity e?e.strongSignal[d.ordinal()]:0;}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.getItem() instanceof PhantomConnector)return InteractionResult.PASS;
        if(stack.is(Items.REDSTONE_TORCH)&&kind==WorkKind.DIGGER){if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof WorkEntity e){e.onlyOres=!e.onlyOres;e.setChanged();player.sendOverlayMessage(Component.literal(e.onlyOres?"Digger: ores only":"Digger: all blocks"));}return InteractionResult.SUCCESS;}
        if(stack.is(Items.REDSTONE_TORCH)&&(kind==WorkKind.FLUID_PLACER||kind==WorkKind.FLUID_COLLECTOR||kind==WorkKind.RECONSTRUCTOR||kind==WorkKind.BREAKER||kind==WorkKind.PLACER||kind==WorkKind.DROPPER||kind==WorkKind.LONG_BREAKER)){
            if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof WorkEntity e){e.pulseMode=!e.pulseMode;e.setChanged();player.sendOverlayMessage(Component.literal(e.pulseMode?"Pulse mode":"Automatic mode"));}
            return InteractionResult.SUCCESS;
        }
        if(level.getBlockEntity(pos) instanceof WorkEntity e&&e.fluidStorage()!=null&&net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil.interactWithFluidStorage(e.fluidStorage(),player,hand))return InteractionResult.SUCCESS;
        return open(level,pos,player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return open(level,pos,player);}
    private InteractionResult open(Level level,BlockPos pos,Player player){if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof WorkEntity e)player.openMenu(e);return InteractionResult.SUCCESS;}
}
