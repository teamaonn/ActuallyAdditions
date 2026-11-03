package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import de.ellpeck.actuallyadditions.fabric.mixin.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
public final class Villages {
 public static final ResourceKey<VillagerProfession> ENGINEER=ResourceKey.create(Registries.VILLAGER_PROFESSION,PowerMod.id("engineer"));
 public static void initialize(){
  var poi=net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper.register(PowerMod.id("engineer"),1,1,Content.MACHINES.get(WorkKind.COFFEE));
  var sound=Registry.register(BuiltInRegistries.SOUND_EVENT,PowerMod.id("villager.work_engineer"),SoundEvent.createVariableRangeEvent(PowerMod.id("villager.work_engineer")));
  var trades=new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<ResourceKey<TradeSet>>();for(int i=1;i<=5;i++)trades.put(i,ResourceKey.create(Registries.TRADE_SET,PowerMod.id("engineer/level_"+i)));
  Registry.register(BuiltInRegistries.VILLAGER_PROFESSION,ENGINEER,new VillagerProfession(Component.translatable("entity.minecraft.villager.actuallyadditions.engineer"),h->h.value()==poi,h->h.value()==poi,ImmutableSet.of(),ImmutableSet.of(),sound,trades));
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server->{
   var pools=server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);var processors=server.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST);var house=processors.getOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST,PowerMod.id("engineer_house")));
   for(var name:List.of("plains","snowy","savanna","taiga","desert")){
    pools.get(ResourceKey.create(Registries.TEMPLATE_POOL,Identifier.withDefaultNamespace("village/"+name+"/houses"))).ifPresent(holder->{var pool=holder.value();int weight=name.equals("desert")?50:10;var piece=StructurePoolElement.legacy("actuallyadditions:andrew_period_house",house).apply(StructureTemplatePool.Projection.RIGID);var access=(PoolAccess)pool;for(int i=0;i<weight;i++)access.aa$templates().add(piece);var raw=new ArrayList<>(pool.getTemplates());raw.add(Pair.of(piece,weight));access.aa$rawTemplates(raw);});
    processors.get(ResourceKey.create(Registries.PROCESSOR_LIST,Identifier.withDefaultNamespace("farm_"+name))).ifPresent(holder->{var rules=new ArrayList<ProcessorRule>();float[] chances={.3F,.25F,.1F,.05F};String[] crops={"rice","coffee","canola","flax"};for(int i=0;i<4;i++)rules.add(new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT,chances[i]),AlwaysTrueTest.INSTANCE,Crops.ALL.get(crops[i]).defaultBlockState()));var list=new ArrayList<>(holder.value().list());list.add(new RuleProcessor(rules));((ProcessorAccess)holder.value()).aa$list(list);});
   }
  });
 }
}
