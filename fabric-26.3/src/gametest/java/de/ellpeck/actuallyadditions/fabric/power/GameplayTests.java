package de.ellpeck.actuallyadditions.fabric.power;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.content.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
public final class GameplayTests {
 static void check(boolean yes,String m){if(!yes)throw new IllegalStateException(m);}
 static WorkEntity machine(GameTestHelper h,WorkKind kind,int x,int z){var p=h.absolutePos(new BlockPos(x,3,z));h.getLevel().setBlock(p,Content.MACHINES.get(kind).defaultBlockState().setValue(PowerBlock.FACING,Direction.EAST),3);return (WorkEntity)h.getLevel().getBlockEntity(p);}
 @GameTest public void machineDropRestoresEnergyAndFluidWithoutCopyingInventory(GameTestHelper h){
  var e=machine(h,WorkKind.PRESS,1,1);e.getEnergyStorage().setEnergy(12345);e.setItem(0,new ItemStack(PowerMod.ITEMS.get("canola"),7));e.outputTank.amount=81000;e.outputTank.variant=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(Oils.ALL.get("canola_oil").source);
  var drops=Block.getDrops(e.getBlockState(),h.getLevel(),e.getBlockPos(),e);check(drops.size()==1,"Machine produces one block drop");var stack=drops.getFirst();check(stack.has(DataComponents.BLOCK_ENTITY_DATA),"Drop contains native typed machine data");
  var p=e.getBlockPos().south(3);h.getLevel().setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);var fake=net.fabricmc.fabric.api.entity.FakePlayer.get(h.getLevel());var previous=fake.getMainHandItem();fake.setItemInHand(InteractionHand.MAIN_HAND,stack);
  try{var result=stack.useOn(new UseOnContext(fake,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p.below()).add(0,.5,0),Direction.UP,p.below(),false)));check(result.consumesAction(),"Machine drop places through native BlockItem");}finally{fake.setItemInHand(InteractionHand.MAIN_HAND,previous);}
  var restored=(WorkEntity)h.getLevel().getBlockEntity(p);check(restored!=null&&restored.energy()==12345&&restored.outputTank.amount==81000,"Native placement restores exact stored power and fluid");check(restored.getItem(0).isEmpty(),"Physical contents are dropped separately and cannot duplicate");h.succeed();
 }
 @GameTest public void breakerPreservesMachineEnergyAndSpillsPhysicalContentsOnce(GameTestHelper h){
  var breaker=machine(h,WorkKind.BREAKER,1,1);var target=machine(h,WorkKind.PRESS,2,1);target.getEnergyStorage().setEnergy(10000);target.setItem(0,new ItemStack(Items.DIAMOND,9));
  check(breaker.worldWork(),"Breaker handles a block with an inventory");check(h.getLevel().getBlockState(target.getBlockPos()).isAir(),"Target removed once");var block=breaker.getItem(0);check(block.is(Content.MACHINES.get(WorkKind.PRESS).asItem())&&block.has(DataComponents.BLOCK_ENTITY_DATA),"Broken machine retains its stored state");int spilled=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(target.getBlockPos()).inflate(2)).stream().filter(i->i.getItem().is(Items.DIAMOND)).mapToInt(i->i.getItem().getCount()).sum();check(spilled==9,"Inventory contents survive exactly once");h.succeed();
 }
 @GameTest public void nativeCropOreAndSlabLootHonorsConditions(GameTestHelper h){
  var p=h.absolutePos(new BlockPos(2,3,2));var crop=Crops.ALL.get("canola");var young=Block.getDrops(crop.getStateForAge(0),h.getLevel(),p,null);check(young.size()==1&&young.getFirst().is(PowerMod.ITEMS.get("canola_seeds")),"Immature crop drops only its seed");var mature=Block.getDrops(crop.getStateForAge(7),h.getLevel(),p,null);int produce=mature.stream().filter(s->s.is(PowerMod.ITEMS.get("canola"))).mapToInt(ItemStack::getCount).sum();check(produce>=2&&produce<=5,"Mature crop uses original random produce counts");var ore=Block.getDrops(Content.BLOCKS.get("black_quartz_ore").defaultBlockState(),h.getLevel(),p,null);check(ore.size()==1&&ore.getFirst().is(PowerMod.ITEMS.get("black_quartz")),"Silk touch branch is conditional");var slab=Content.BLOCKS.get("black_quartz_slab").defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);check(Block.getDrops(slab,h.getLevel(),p,null).getFirst().getCount()==2,"Double slab drops both halves");h.succeed();
 }
 @GameTest public void cachedPhantomViewsCannotAccessReboundInventory(GameTestHelper h){
  var phantom=machine(h,WorkKind.PHANTOM_ITEM,1,1);var one=phantom.getBlockPos().east(2);var two=one.south(2);h.getLevel().setBlock(one,Blocks.CHEST.defaultBlockState(),3);h.getLevel().setBlock(two,Blocks.CHEST.defaultBlockState(),3);((Container)h.getLevel().getBlockEntity(one)).setItem(0,new ItemStack(Items.DIAMOND,8));phantom.bound=one;var storage=phantom.itemStorage(Direction.UP);var view=storage.iterator().next();check(view.getAmount()==8,"Bound inventory is visible");phantom.bound=two;try(var tx=Transaction.openOuter()){check(view.extract(ItemVariant.of(Items.DIAMOND),1,tx)==0,"Old view refuses extraction after rebinding");tx.commit();}check(((Container)h.getLevel().getBlockEntity(one)).getItem(0).getCount()==8,"Old inventory stays intact");h.succeed();
 }
 @GameTest public void lightingChargesOnlyForSuccessfulActions(GameTestHelper h){
  var shock=machine(h,WorkKind.SHOCK,1,1);shock.getEnergyStorage().setEnergy(900);shock.tick();var near=shock.getBlockPos().east(2);var far=shock.getBlockPos().east(8);var result=Lighting.protect(h.getLevel(),List.of(near,far));check(!result.contains(near)&&result.contains(far)&&shock.energy()==600,"Suppressor protects only in-range blocks for 300 energy each");var fire=machine(h,WorkKind.FIREWORK,3,3);check(!fire.lighting.launch(),"Empty Firework Box cannot launch");fire.getEnergyStorage().setEnergy(1000);check(fire.lighting.launch()&&fire.energy()==500,"Successful launch uses exactly 500 energy");h.succeed();
 }
 @GameTest public void wormsMoistenSoilAndSacksConserveOverflow(GameTestHelper h){
  var p=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlock(p,Blocks.FARMLAND.defaultBlockState(),3);var worm=new WormEntity(WormEntity.TYPE,h.getLevel());worm.setPos(p.getX()+.5,p.getY()+.5,p.getZ()+.5);h.getLevel().addFreshEntity(worm);for(int i=0;i<50;i++)worm.tick();check(h.getLevel().getBlockState(p).getValue(FarmlandBlock.MOISTURE)==7,"Worm keeps center farmland watered");var sack=new ItemStack(PowerMod.ITEMS.get("travelers_sack"));check(SackItem.insert(sack,new ItemStack(Items.DIAMOND,1792))==1792,"Sack exposes all 28 real slots");check(SackItem.insert(sack,new ItemStack(Items.DIAMOND,1))==0,"A full sack leaves overflow untouched");check(SackItem.insert(new ItemStack(PowerMod.ITEMS.get("travelers_sack")),sack)==0,"Sacks cannot be nested");h.succeed();
 }
 @GameTest public void engineerUsesNativePoiTradesAndVillagePool(GameTestHelper h){
  var access=h.getLevel().registryAccess();var profession=access.lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(Villages.ENGINEER).value();check(profession.getTrades(1)!=null,"Engineer has native trade-set keys");var trades=access.lookupOrThrow(Registries.TRADE_SET).getOrThrow(profession.getTrades(1)).value();check(trades.trades().size()==3,"All original novice trades migrate");var pool=access.lookupOrThrow(Registries.TEMPLATE_POOL).getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,net.minecraft.resources.Identifier.withDefaultNamespace("village/plains/houses"))).value();check(pool.getTemplates().stream().anyMatch(p->p.getSecond()==10&&p.getFirst().toString().contains("andrew_period_house")),"Engineer house participates in native village selection");h.succeed();
 }
}
