package de.ellpeck.actuallyadditions.fabric.content;
import de.ellpeck.actuallyadditions.fabric.power.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
public final class Decorations {
    public static void initialize(){
        for(var name:java.util.List.of("restonia","palis","diamatine","void","emeradic","enori"))Content.block(name+"_crystal_cluster",p->new AmethystClusterBlock(19,2,p),BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(s->7));
        Content.block("tiny_torch",TinyTorch::new,BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH).lightLevel(s->12));
        for(var color:DyeColor.values())Content.block("lamp_"+color.getName(),Lamp::new,BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).lightLevel(s->s.getValue(PowerBlock.ACTIVE)?15:0));
        Content.block("greenhouse_glass",Greenhouse::new,BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).randomTicks());
    }
    public static final class Lamp extends Block {
        Lamp(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(PowerBlock.ACTIVE,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(PowerBlock.ACTIVE);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide())l.setBlockAndUpdate(p,s.cycle(PowerBlock.ACTIVE));return InteractionResult.SUCCESS;}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
            if(!(stack.getItem() instanceof DyeItem dye))return InteractionResult.PASS;
            var lamp=Content.BLOCKS.get("lamp_"+stack.get(net.minecraft.core.component.DataComponents.DYE).getName());if(lamp==this)return InteractionResult.PASS;
            if(!l.isClientSide()){l.setBlockAndUpdate(p,lamp.defaultBlockState().setValue(PowerBlock.ACTIVE,s.getValue(PowerBlock.ACTIVE)));if(!player.isCreative())stack.shrink(1);}return InteractionResult.SUCCESS;
        }
    }
    public static final class Greenhouse extends TransparentBlock {
        Greenhouse(Properties p){super(p);}
        @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos pos,RandomSource random){
            if(!l.canSeeSky(pos)||!l.isBrightOutside())return;
            for(var p=pos.below();p.getY()>=l.getMinY();p=p.below()){
                var state=l.getBlockState(p);
                if(state.getBlock() instanceof BonemealableBlock plant){
                    for(int i=0;i<3;i++){var current=l.getBlockState(p);if(current.getBlock()!=plant||!plant.isValidBonemealTarget(l,p,current,BonemealSource.INTERACTION))break;plant.performBonemeal(l,random,p,current,BonemealSource.INTERACTION);}
                    l.levelEvent(2005,p,0);return;
                }
                if(state.isSolidRender()||state.is(this))return;
            }
        }
    }
}
