package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class Crops {
    public static final Map<String,Crop> ALL=new LinkedHashMap<>();
    public static void initialize(){
        for(var name:List.of("canola","rice","flax","coffee")){
            var seed=name.equals("coffee")?"coffee_beans":name+"_seeds";
            var key=ResourceKey.create(Registries.BLOCK,PowerMod.id(name));
            var crop=Registry.register(BuiltInRegistries.BLOCK,key,new Crop(seed,BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).setId(key)));ALL.put(name,crop);Content.BLOCKS.put(name,crop);
            Content.item(seed,p->new BlockItem(crop,p),new Item.Properties());
        }
        Content.item("crystallized_canola_seed",Item::new,new Item.Properties());
        Content.item("empowered_canola_seed",Item::new,new Item.Properties().component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(level->{
            for(var entity:level.getAllEntities())if(entity instanceof ItemEntity item&&item.isAlive())upgradeOil(level,item);
        });
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY_DROPS.register((table,context,drops)->{
            var key=table.unwrapKey();if(key.isEmpty())return;String name=key.get().identifier().toString();
            if((name.equals("minecraft:blocks/short_grass")||name.equals("minecraft:blocks/tall_grass"))&&context.getRandom().nextInt(16)==0){
                var seeds=List.of("canola_seeds","rice_seeds","flax_seeds","coffee_beans");drops.add(new ItemStack(PowerMod.ITEMS.get(seeds.get(context.getRandom().nextInt(seeds.size())))));
            }
        });
    }
    public static boolean upgradeOil(ServerLevel level,ItemEntity item){
        var stack=item.getItem();boolean empowered=stack.is(PowerMod.ITEMS.get("empowered_canola_seed"));
        if(!empowered&&!stack.is(PowerMod.ITEMS.get("crystallized_canola_seed")))return false;
        var old=Oils.ALL.get(empowered?"crystallized_oil":"refined_canola_oil");var result=Oils.ALL.get(empowered?"empowered_oil":"crystallized_oil");
        var pos=item.blockPosition();var state=level.getFluidState(pos);
        if(!state.isSource()||!state.is(old.source))return false;
        if(level.setBlockAndUpdate(pos,result.block.defaultBlockState())){stack.shrink(1);if(stack.isEmpty())item.discard();else item.setItem(stack);return true;}return false;
    }
    public static class Crop extends CropBlock {
        public static final BooleanProperty PERSISTENT=BooleanProperty.create("persistent");private final String seed;
        Crop(String seed,Properties properties){super(properties);this.seed=seed;registerDefaultState(stateDefinition.any().setValue(AGE,0).setValue(PERSISTENT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AGE,PERSISTENT);}
        @Override protected ItemLike getBaseSeedId(){return PowerMod.ITEMS.get(seed);}
        @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos){return state.is(Blocks.FARMLAND);}
        @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return state.getValue(PERSISTENT)&&!level.getBlockState(pos.below()).isAir()||super.canSurvive(state,level,pos);}
        private InteractionResult harvest(BlockState state,Level world,BlockPos pos,Player player){
            if(!isMaxAge(state))return InteractionResult.PASS;
            if(world instanceof ServerLevel server){boolean seedTaken=false;for(var drop:Block.getDrops(state,server,pos,null,player,player.getMainHandItem())){
                if(!seedTaken&&drop.is(PowerMod.ITEMS.get(seed))){drop.shrink(1);seedTaken=true;}
                if(!drop.isEmpty()&&!player.getInventory().add(drop))Block.popResource(world,pos,drop);
            }world.setBlockAndUpdate(pos,getStateForAge(0));}return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return harvest(state,world,pos,player);}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level world,BlockPos pos,Player player,BlockHitResult hit){return harvest(state,world,pos,player);}
    }
}
