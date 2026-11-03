package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;
public final class WormEntity extends ItemEntity {
 public static EntityType<WormEntity> TYPE;private int timer;private record Tilled(ServerLevel level,BlockPos pos){}private static final Set<Tilled> TILLING=new HashSet<>();
 public WormEntity(EntityType<? extends WormEntity> type,Level l){super(type,l);setItem(new ItemStack(PowerMod.ITEMS.get("worm")));setNoGravity(true);setPickUpDelay(Integer.MAX_VALUE);}
 public static boolean valid(Level l,BlockPos p){var s=l.getBlockState(p);var above=l.getBlockState(p.above());return (s.getBlock() instanceof FarmlandBlock||s.is(Blocks.DIRT)||s.is(Blocks.GRASS_BLOCK))&&(above.canBeReplaced()||above.getBlock() instanceof VegetationBlock);}
 @Override public void playerTouch(Player p){}
 @Override public void tick(){if(!(level() instanceof ServerLevel l))return;timer++;if(timer%50!=0)return;var center=blockPosition();if(!valid(l,center)){discard();return;}for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var p=center.offset(x,0,z);if(!l.hasChunkAt(p)||!valid(l,p))continue;var s=l.getBlockState(p);if(s.getBlock() instanceof FarmlandBlock){if(s.getValue(FarmlandBlock.MOISTURE)<7)l.setBlockAndUpdate(p,s.setValue(FarmlandBlock.MOISTURE,7));if(l.getRandom().nextFloat()>=.95F){var plant=p.above();var before=l.getBlockState(plant);if(before.getBlock() instanceof BonemealableBlock&&!(before.getBlock() instanceof GrassBlock)){before.randomTick(l,plant,l.getRandom());if(l.getBlockState(plant)!=before)l.levelEvent(2005,plant,0);}}}else if(x==0&&z==0||l.getRandom().nextFloat()>=.45F)l.setBlockAndUpdate(p,Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));}}
 @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("worm_timer",timer);}
 @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);timer=Math.max(0,in.getIntOr("worm_timer",0));setNoGravity(true);setPickUpDelay(Integer.MAX_VALUE);}
 public static void initialize(){
  var key=ResourceKey.create(Registries.ENTITY_TYPE,PowerMod.id("worm"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WormEntity>of(WormEntity::new,MobCategory.MISC).sized(.25F,.1F).clientTrackingRange(16).updateInterval(20).build(key));
  Content.item("worm",p->new Item(p){@Override public InteractionResult useOn(UseOnContext c){var l=c.getLevel();var pos=c.getClickedPos();if(!valid(l,pos)||!l.getEntitiesOfClass(WormEntity.class,new AABB(pos).inflate(1,0,1)).isEmpty())return InteractionResult.PASS;if(l.isClientSide())return InteractionResult.SUCCESS;var worm=new WormEntity(TYPE,l);worm.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);worm.setCustomName(c.getItemInHand().getHoverName());if(l.addFreshEntity(worm)&&c.getPlayer()!=null&&!c.getPlayer().isCreative())c.getItemInHand().shrink(1);return InteractionResult.SUCCESS;}},new Item.Properties());
  net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p,l,h,hit)->{var s=p.getItemInHand(h);var pos=hit.getBlockPos();if(l instanceof ServerLevel server&&(s.getItem() instanceof UtilityItems.Aio||s.is(net.minecraft.tags.TagKey.create(Registries.ITEM,net.minecraft.resources.Identifier.withDefaultNamespace("hoes"))))&&(l.getBlockState(pos).is(Blocks.DIRT)||l.getBlockState(pos).is(Blocks.GRASS_BLOCK)))TILLING.add(new Tilled(server,pos.immutable()));return InteractionResult.PASS;});
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{for(var c:TILLING)if(c.level.hasChunkAt(c.pos)&&c.level.getBlockState(c.pos).is(Blocks.FARMLAND)&&c.level.getRandom().nextFloat()<.05F)Block.popResource(c.level,c.pos.above(),new ItemStack(PowerMod.ITEMS.get("worm"),1+c.level.getRandom().nextInt(2)));TILLING.clear();});
 }
}
