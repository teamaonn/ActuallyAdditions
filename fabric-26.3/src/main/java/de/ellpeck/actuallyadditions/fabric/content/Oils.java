package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.*;

/** Non-renewable oil sources, flowing states and native bucket interactions. */
public final class Oils {
    public static final Map<String,Family> ALL=new LinkedHashMap<>();
    public static final class Family {
        public final String name;public Oil source,flowing;public LiquidBlock block;public Item bucket;
        Family(String name){this.name=name;}
    }
    public static void initialize(){
        for(var name:List.of("canola_oil","refined_canola_oil","crystallized_oil","empowered_oil")){
            var f=new Family(name);ALL.put(name,f);
            f.source=Registry.register(BuiltInRegistries.FLUID,PowerMod.id(name),new Oil(f,true));
            f.flowing=Registry.register(BuiltInRegistries.FLUID,PowerMod.id(name+"_flowing"),new Oil(f,false));
            var key=ResourceKey.create(Registries.BLOCK,PowerMod.id(name));
            f.block=Registry.register(BuiltInRegistries.BLOCK,key,new LiquidBlock(f.source,BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).setId(key)));
            Content.item(name+"_bucket",p->new BucketItem(f.source,p.stacksTo(1).craftRemainder(Items.BUCKET)),new Item.Properties());f.bucket=PowerMod.ITEMS.get(name+"_bucket");
        }
    }
    public static class Oil extends FlowingFluid {
        private final Family family;private final boolean source;
        Oil(Family family,boolean source){this.family=family;this.source=source;registerDefaultState(stateDefinition.any().setValue(FALLING,false).setValue(LEVEL,source?8:7));}
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState> b){super.createFluidStateDefinition(b);b.add(LEVEL);}
        @Override public Fluid getFlowing(){return family.flowing;}
        @Override public Fluid getSource(){return family.source;}
        @Override public Item getBucket(){return family.bucket;}
        @Override protected boolean canConvertToSource(ServerLevel level){return false;}
        @Override protected void beforeDestroyingBlock(LevelAccessor level,BlockPos pos,BlockState state){Block.dropResources(state,level,pos,level.getBlockEntity(pos));}
        @Override protected int getSlopeFindDistance(LevelReader level){return 4;}
        @Override protected int getDropOff(LevelReader level){return 1;}
        @Override public int getTickDelay(LevelReader level){return 5;}
        @Override protected float getExplosionResistance(){return 100F;}
        @Override protected boolean canBeReplacedWith(FluidState state,BlockGetter level,BlockPos pos,Fluid fluid,Direction dir){return dir==Direction.DOWN&&!isSame(fluid);}
        @Override protected BlockState createLegacyBlock(FluidState state){return family.block.defaultBlockState().setValue(LiquidBlock.LEVEL,getLegacyLevel(state));}
        @Override public boolean isSource(FluidState state){return source;}
        @Override public int getAmount(FluidState state){return source?8:state.getValue(LEVEL);}
        @Override public boolean isSame(Fluid other){return other==family.source||other==family.flowing;}
        @Override public Optional<net.minecraft.sounds.SoundEvent> getPickupSound(){return Optional.of(net.minecraft.sounds.SoundEvents.BUCKET_FILL);}
    }
}
