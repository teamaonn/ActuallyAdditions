package de.ellpeck.actuallyadditions.fabric.power;
import de.ellpeck.actuallyadditions.fabric.content.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
public final class TransportTests {
 static void check(boolean yes,String message){if(!yes)throw new IllegalStateException(message);}
 static WorkEntity machine(GameTestHelper h,WorkKind kind,int x,int z){var p=h.absolutePos(new BlockPos(x,2,z));h.getLevel().setBlock(p,Content.MACHINES.get(kind).defaultBlockState(),3);return (WorkEntity)h.getLevel().getBlockEntity(p);}
 static Container chest(GameTestHelper h,BlockPos pos){h.getLevel().setBlock(pos,Blocks.CHEST.defaultBlockState(),3);return (Container)h.getLevel().getBlockEntity(pos);}
 @GameTest(padding=24) public void itemNetworkHonorsFiltersRollbackAndDisconnectedViews(GameTestHelper h){
  var a=machine(h,WorkKind.ITEM_RELAY,1,1);var b=machine(h,WorkKind.ADVANCED_ITEM_RELAY,9,1);var c=machine(h,WorkKind.ITEM_RELAY,9,9);var access=machine(h,WorkKind.ITEM_INTERFACE,1,2);
  Transport.connect(a.network,b.network);Transport.connect(b.network,c.network);Transport.connect(c.network,a.network);b.network.priority=10;
  var filter=new ItemStack(PowerMod.ITEMS.get("filter"));filter.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));b.setItem(0,filter);
  var first=chest(h,b.getBlockPos().east());var second=chest(h,c.getBlockPos().east());var storage=Transport.items(access);
  try(var tx=Transaction.openOuter()){check(storage.insert(ItemVariant.of(Items.DIAMOND),32,tx)==32,"Network must accept diamonds");}
  check(first.isEmpty()&&second.isEmpty(),"Aborting an insertion must leave every inventory unchanged");
  try(var tx=Transaction.openOuter()){check(storage.insert(ItemVariant.of(Items.DIRT),16,tx)==16,"Lower-priority unfiltered endpoint accepts dirt");check(storage.insert(ItemVariant.of(Items.DIAMOND),32,tx)==32,"Highest-priority allowed endpoint accepts diamonds");tx.commit();}
  check(first.getItem(0).is(Items.DIAMOND)&&first.getItem(0).getCount()==32&&second.getItem(0).is(Items.DIRT),"Filters and priorities choose the correct inventories");
  net.fabricmc.fabric.api.transfer.v1.storage.StorageView<ItemVariant> saved=null;for(var view:storage)if(view.getResource().isOf(Items.DIAMOND)){saved=view;break;}check(saved!=null,"Network exposes real item views");b.network.unlink();
  try(var tx=Transaction.openOuter()){check(saved.extract(ItemVariant.of(Items.DIAMOND),1,tx)==0,"A cached view cannot extract from a disconnected relay");tx.commit();}check(first.getItem(0).getCount()==32,"Disconnect preserves the inaccessible inventory");h.succeed();
 }
 @GameTest(padding=24) public void fluidRollbackRestoresSharedPerTickBudget(GameTestHelper h){
  var a=machine(h,WorkKind.FLUID_RELAY,1,1);var b=machine(h,WorkKind.FLUID_RELAY,9,1);var one=machine(h,WorkKind.FLUID_PLACER,10,1);var two=machine(h,WorkKind.FLUID_PLACER,9,2);Transport.connect(a.network,b.network);
  var fluid=FluidVariant.of(Oils.ALL.get("canola_oil").source);var storage=a.fluidStorage(Direction.WEST);
  try(var tx=Transaction.openOuter()){check(storage.insert(fluid,810000,tx)==810000,"Fluid network can route 10 buckets");}
  check(one.inputTank.amount==0&&two.inputTank.amount==0,"Aborted fluid transfer rolls back both sinks");
  try(var tx=Transaction.openOuter()){check(storage.insert(fluid,810000,tx)==810000,"Rollback must also restore path throughput budgets");tx.commit();}
  check(one.inputTank.amount+two.inputTank.amount==810000,"Committed transfer conserves exact fluid units");try(var tx=Transaction.openOuter()){check(storage.insert(fluid,81,tx)==0,"All calls share the same per-tick budget");tx.commit();}h.succeed();
 }
 @GameTest(padding=24) public void replacedNodeCannotInheritOldConnections(GameTestHelper h){
  var a=machine(h,WorkKind.ITEM_RELAY,1,1);var b=machine(h,WorkKind.ITEM_RELAY,9,1);var access=machine(h,WorkKind.ITEM_INTERFACE,1,2);Transport.connect(a.network,b.network);var old=b.network.id;var inventory=chest(h,b.getBlockPos().east());inventory.setItem(0,new ItemStack(Items.DIAMOND));
  h.getLevel().setBlock(b.getBlockPos(),Blocks.AIR.defaultBlockState(),3);var replacement=machine(h,WorkKind.ITEM_RELAY,9,1);a.network.links.put(replacement.getBlockPos(),old);replacement.network.links.put(a.getBlockPos(),a.network.id);
  try(var tx=Transaction.openOuter()){check(Transport.items(access).extract(ItemVariant.of(Items.DIAMOND),1,tx)==0,"UUID validation rejects stale links to replaced blocks");tx.commit();}check(inventory.getItem(0).getCount()==1,"Stale links cannot alter a remote chest");h.succeed();
 }
}
