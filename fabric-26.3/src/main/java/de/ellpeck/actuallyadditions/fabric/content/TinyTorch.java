package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.*;
public final class TinyTorch extends Block {
 public static final EnumProperty<Direction> FACING=EnumProperty.create("facing",Direction.class,d->d!=Direction.DOWN);
 public TinyTorch(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){for(var d:c.getNearestLookingDirections()){d=d.getOpposite();if(d==Direction.DOWN)continue;var s=defaultBlockState().setValue(FACING,d);if(s.canSurvive(c.getLevel(),c.getClickedPos()))return s;}return null;}
 @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var d=s.getValue(FACING);var support=p.relative(d.getOpposite());return d==Direction.UP?canSupportCenter(l,support,Direction.UP):l.getBlockState(support).isFaceSturdy(l,support,d);}
 @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos adjacent,BlockState state,RandomSource r){return s.canSurvive(l,p)?s:Blocks.AIR.defaultBlockState();}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(FACING)){case EAST->Block.box(0,4,6,6,11,10);case WEST->Block.box(10,4,6,16,11,10);case SOUTH->Block.box(6,4,0,10,11,6);case NORTH->Block.box(6,4,10,10,11,16);default->Block.box(6,0,6,10,6,10);};}
 @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){var d=s.getValue(FACING);l.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,p.getX()+.5-d.getStepX()*.25,p.getY()+(d==Direction.UP?.4:.7),p.getZ()+.5-d.getStepZ()*.25,0,0,0);}
}
