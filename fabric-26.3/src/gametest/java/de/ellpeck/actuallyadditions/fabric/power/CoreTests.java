package de.ellpeck.actuallyadditions.fabric.power;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import de.ellpeck.actuallyadditions.fabric.content.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public final class CoreTests {
    private static void check(boolean yes,String text){if(!yes)throw new IllegalStateException(text);}
    private static ItemStack item(String name){return new ItemStack(PowerMod.ITEMS.get(name));}
    private static WorkEntity machine(GameTestHelper h,WorkKind kind,int x,int y,int z){
        var pos=h.absolutePos(new BlockPos(x,y,z));h.getLevel().setBlock(pos,Content.MACHINES.get(kind).defaultBlockState().setValue(PowerBlock.FACING,Direction.EAST),3);return (WorkEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void run(WorkEntity e,int ticks){for(int i=0;i<ticks;i++)e.tick();}
    @GameTest public void reconstructorConvertsItemsAndChargesExactEnergy(GameTestHelper h){
        var e=machine(h,WorkKind.RECONSTRUCTOR,1,2,1);e.getEnergyStorage().setEnergy(10000);
        h.getLevel().setBlock(e.getBlockPos().east(4),Blocks.BEDROCK.defaultBlockState(),3);
        var input=new ItemEntity(h.getLevel(),e.getBlockPos().getX()+2.5,e.getBlockPos().getY()+.5,e.getBlockPos().getZ()+.5,new ItemStack(Items.REDSTONE,16));input.setNoGravity(true);h.getLevel().addFreshEntity(input);
        check(e.fire(),"Reconstructor must fire with sufficient energy");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(e.getBlockPos()).inflate(5));
        int count=drops.stream().filter(x->x.getItem().is(PowerMod.ITEMS.get("restonia_crystal"))).mapToInt(x->x.getItem().getCount()).sum();
        check(count==16,"16 redstone must yield exactly 16 Restonia crystals");check(!input.isAlive(),"Original input entity consumed once");
        check(e.energy()==10000-1000-16*40,"Charge 1000 per pulse and upstream recipe energy per converted item");h.succeed();
    }
    @GameTest public void reconstructorConvertsBlocksAndPreservesInventories(GameTestHelper h){
        var e=machine(h,WorkKind.RECONSTRUCTOR,1,2,1);e.getEnergyStorage().setEnergy(10000);
        var target=e.getBlockPos().east(3);h.getLevel().setBlock(target,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
        var recipe=Processing.find(h.getLevel(),"laser",new ItemStack(Items.DIAMOND_BLOCK)).orElseThrow();int cost=recipe.number("energy",0);e.fire();
        check(h.getLevel().getBlockState(target).is(Content.BLOCKS.get("diamatine_crystal_block")),"Diamond block converts to Diamatine block");check(e.energy()==9000-cost,"Block conversion charges actual recipe cost");
        h.getLevel().setBlock(target,Blocks.CHEST.defaultBlockState(),3);e.fire();check(h.getLevel().getBlockState(target).is(Blocks.CHEST),"Beam cannot erase block inventories");h.succeed();
    }
    @GameTest public void crushersProcessRealUpstreamOreRecipes(GameTestHelper h){
        var a=machine(h,WorkKind.CRUSHER,1,2,1);a.setItem(0,new ItemStack(Items.IRON_ORE,2));a.getEnergyStorage().setEnergy(60000);run(a,100);
        check(a.getItem(0).getCount()==1&&a.getItem(1).is(Items.RAW_IRON)&&a.getItem(1).getCount()==2,"Crusher doubles iron ore using the live upstream recipe");check(a.energy()==56000,"Crusher uses 40 energy for 100 ticks");
        var b=machine(h,WorkKind.DOUBLE_CRUSHER,5,2,1);b.setItem(0,new ItemStack(Items.IRON_ORE));b.setItem(3,new ItemStack(Items.GOLD_ORE));b.getEnergyStorage().setEnergy(60000);run(b,150);
        check(b.getItem(1).is(Items.RAW_IRON)&&b.getItem(4).is(Items.RAW_GOLD),"Double Crusher processes both lanes independently");check(b.energy()==48000,"Double Crusher charges both active lanes");h.succeed();
    }
    @GameTest public void poweredFurnaceUsesTwoVanillaSmeltingLanes(GameTestHelper h){
        var e=machine(h,WorkKind.FURNACE,1,2,1);e.setItem(0,new ItemStack(Items.RAW_IRON));e.setItem(3,new ItemStack(Items.RAW_GOLD));e.getEnergyStorage().setEnergy(30000);run(e,80);
        check(e.getItem(1).is(Items.IRON_INGOT)&&e.getItem(4).is(Items.GOLD_INGOT),"Powered Furnace completes vanilla recipes in both lanes");check(e.energy()==26000,"Furnace uses 25 energy per lane for 80 ticks");h.succeed();
    }
    @GameTest public void blockedOutputsAndSaveRestoreCannotRerollInputs(GameTestHelper h){
        var e=machine(h,WorkKind.CRUSHER,1,2,1);e.setItem(0,new ItemStack(Items.IRON_ORE));e.setItem(1,new ItemStack(Items.DIAMOND,64));e.setItem(2,new ItemStack(Items.DIAMOND,64));e.getEnergyStorage().setEnergy(60000);run(e,150);
        check(e.getItem(0).getCount()==1&&e.energy()==60000,"Blocked outputs consume neither input nor power");e.setItem(1,ItemStack.EMPTY);e.setItem(2,ItemStack.EMPTY);run(e,40);
        var copy=(WorkEntity)BlockEntity.loadStatic(e.getBlockPos(),e.getBlockState(),e.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());copy.setLevel(h.getLevel());
        check(copy.progress(0)==40&&copy.energy()==58400,"Work progress and energy survive reload");run(copy,60);
        check(copy.getItem(0).isEmpty()&&copy.getItem(1).getCount()==2&&copy.energy()==56000,"Restored crusher completes exactly one operation");h.succeed();
    }
    @GameTest public void empowererMatchesUnorderedStandsAndChargesEach(GameTestHelper h){
        var e=machine(h,WorkKind.EMPOWERER,4,2,4);e.setItem(0,item("emeradic_crystal"));
        List<WorkEntity> stands=new ArrayList<>();List<ItemStack> modifiers=List.of(new ItemStack(Items.SLIME_BALL),new ItemStack(Items.OAK_SAPLING),new ItemStack(Items.SHORT_GRASS),new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("minecraft:lime_dye"))));
        int i=0;for(var direction:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var p=new BlockPos(4,2,4).relative(direction,3);var stand=machine(h,WorkKind.DISPLAY,p.getX(),p.getY(),p.getZ());stand.setItem(0,modifiers.get(i++));stand.getEnergyStorage().setEnergy(5000);stands.add(stand);
        }
        run(e,50);check(e.getItem(0).is(PowerMod.ITEMS.get("empowered_emeradic_crystal")),"Empowerer uses its original four-stand recipe regardless of ingredient order");
        for(var stand:stands)check(stand.energy()==0&&stand.getItem(0).isEmpty(),"Each display pays exactly 5000 energy and one modifier");h.succeed();
    }
    @GameTest public void batteriesChargeDischargeAndRetainState(GameTestHelper h){
        var e=machine(h,WorkKind.ENERGIZER,1,2,1);var battery=item("single_battery");e.setItem(0,battery);e.getEnergyStorage().setEnergy(50000);run(e,20);
        check(EnergyItem.energy(battery)==20000&&e.energy()==30000,"Energizer transfers exactly 1000 per tick into battery");
        var saved=(WorkEntity)BlockEntity.loadStatic(e.getBlockPos(),e.getBlockState(),e.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());saved.setLevel(h.getLevel());
        check(EnergyItem.energy(saved.getItem(0))==20000,"Charged battery retains charge in saved machine");
        var drain=machine(h,WorkKind.ENERVATOR,5,2,1);drain.setItem(0,saved.removeItemNoUpdate(0));run(drain,20);
        check(drain.energy()==20000&&drain.getItem(0).isEmpty()&&EnergyItem.energy(drain.getItem(1))==0,"Enervator transfers stored energy without creating or losing charge");
        var box=machine(h,WorkKind.BATTERY_BOX,9,2,1);box.setItem(0,item("double_battery"));
        check(box.getEnergyStorage().fill(100000,true)==5000&&box.energy()==0,"Battery box simulation respects tier without changing charge");
        check(box.getEnergyStorage().fill(100000,false)==5000&&box.getEnergyStorage().fill(1,false)==0,"Battery box shares one per-tick input budget");
        check(box.getEnergyStorage().drain(100000,false)==5000&&box.energy()==0,"Battery box exposes actual battery charge");h.succeed();
    }
    @GameTest public void generatorsUseHeatLeavesAndDistinctBiomass(GameTestHelper h){
        var heat=machine(h,WorkKind.HEAT,4,3,4);
        for(var d:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST))h.getLevel().setBlock(heat.getBlockPos().relative(d),Blocks.MAGMA_BLOCK.defaultBlockState(),3);
        heat.tick();check(heat.energy()==0,"Heat Collector needs four heat sources");h.getLevel().setBlock(heat.getBlockPos().below(),Blocks.MAGMA_BLOCK.defaultBlockState(),3);heat.tick();check(heat.energy()==40,"Four heat sources produce upstream 40 energy per tick");
        var leaf=machine(h,WorkKind.LEAF,15,3,4);var pos=leaf.getBlockPos().above();h.getLevel().setBlock(pos,Blocks.OAK_LEAVES.defaultBlockState(),3);run(leaf,5);
        check(leaf.energy()==300&&h.getLevel().isEmptyBlock(pos),"Leaf Generator consumes one leaf for 300 energy");
        var bio=machine(h,WorkKind.BIO,26,3,4);bio.setItem(0,new ItemStack(Items.CARROT,2));bio.setItem(1,new ItemStack(Items.CARROT,2));bio.setItem(2,new ItemStack(Items.APPLE,2));bio.tick();
        check(bio.getItem(0).getCount()==1&&bio.getItem(1).getCount()==2&&bio.getItem(2).getCount()==1,"Bioreactor consumes only one of each distinct biomass type");bio.tick();check(bio.energy()==16,"Two distinct biomass inputs produce 16 energy per tick");h.succeed();
    }
    @GameTest public void breakerPlacerAndDropperMoveItemsOnce(GameTestHelper h){
        var breaker=machine(h,WorkKind.BREAKER,1,2,1);var target=breaker.getBlockPos().east();h.getLevel().setBlock(target,Blocks.DIRT.defaultBlockState(),3);
        check(breaker.worldWork()&&h.getLevel().isEmptyBlock(target)&&breaker.getItem(0).is(Items.DIRT),"Breaker stores block loot once");check(!breaker.worldWork(),"Empty space cannot create more loot");
        var placer=machine(h,WorkKind.PLACER,5,2,1);placer.setItem(0,new ItemStack(Items.DIRT,2));check(placer.worldWork(),"Placer invokes native item placement");check(h.getLevel().getBlockState(placer.getBlockPos().east()).is(Blocks.DIRT)&&placer.getItem(0).getCount()==1,"Placer consumes exactly one placed block");
        var dropper=machine(h,WorkKind.DROPPER,9,2,1);dropper.setItem(0,new ItemStack(Items.DIAMOND,2));check(dropper.worldWork()&&dropper.getItem(0).getCount()==1,"Dropper emits one item per cycle");h.succeed();
    }

    @GameTest public void oilChainUsesTransactionsAndExactUpstreamVolumes(GameTestHelper h){
        var press=machine(h,WorkKind.PRESS,1,2,1);press.setItem(0,item("canola"));press.getEnergyStorage().setEnergy(40000);run(press,30);
        check(press.getItem(0).isEmpty()&&press.outputTank.amount==80L*81&&press.energy()==38950,"Press converts one canola to 80 mB for 30 ticks at 35 energy");
        var ferment=machine(h,WorkKind.FERMENT,6,2,1);var canola=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(Oils.ALL.get("canola_oil").source);
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){check(ferment.inputTank.insert(canola,80L*81,tx)==80L*81,"Barrel accepts canola oil");}
        check(ferment.inputTank.amount==0,"Aborted transfer rolls back fluid state");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(press.outputTank,ferment.inputTank,r->true,80L*81,tx);tx.commit();}
        check(press.outputTank.amount==0&&ferment.inputTank.amount==80L*81,"Committed transfer conserves fluid between machines");run(ferment,100);
        check(ferment.inputTank.amount==0&&ferment.outputTank.amount==80L*81&&ferment.outputTank.variant.getFluid()==Oils.ALL.get("refined_canola_oil").source,"Barrel ferments 80 mB into 80 mB refined oil");
        var oil=machine(h,WorkKind.OIL,11,2,1);
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(ferment.outputTank,oil.inputTank,r->true,80L*81,tx);tx.commit();}
        run(oil,121);check(oil.energy()==9600&&oil.inputTank.amount==30L*81,"Oil Generator consumes 50 mB refined oil for exactly 9600 energy");h.succeed();
    }
    @GameTest public void batteryUpgradeCraftingPreservesCharge(GameTestHelper h){
        var charged=item("single_battery");EnergyItem.set(charged,123456);EnergyItem.toggle(charged);
        var grid=net.minecraft.world.item.crafting.CraftingInput.of(3,3,List.of(ItemStack.EMPTY,charged,ItemStack.EMPTY,item("enori_crystal"),item("advanced_coil"),item("enori_crystal"),item("enori_crystal"),item("enori_crystal"),item("enori_crystal")));
        var recipe=h.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();var result=recipe.value().assemble(grid);
        check(result.is(PowerMod.ITEMS.get("double_battery"))&&EnergyItem.energy(result)==123456&&EnergyItem.enabled(result),"Native battery upgrade recipe retains charge and enabled state");h.succeed();
    }

}
