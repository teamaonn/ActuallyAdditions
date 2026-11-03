package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
public final class PortableData {
 public static List<ItemStack> attach(BlockState state,LootParams.Builder params,List<ItemStack> drops){
  var entity=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);if(entity==null||entity.getLevel()==null)return drops;var data=entity.saveWithFullMetadata(entity.getLevel().registryAccess());for(String key:List.of("x","y","z","fuel","node","links","range_upgrade","invisible"))data.remove(key);
  for(int i=0;i<12;i++)data.remove("slot_"+i);for(int i=0;i<2;i++)for(String key:List.of("pending_","input_","job_","progress_"))data.remove(key+i);
  if(entity instanceof WorkEntity e&&e.isTransportRelay()){var network=new CompoundTag();network.putInt("mode",e.network.mode);network.putInt("priority",e.network.priority);data.put("network",network);}
  if(entity instanceof WorkEntity e&&e.isPhantom())data.putString("bound_dimension",entity.getLevel().dimension().identifier().toString());
  for(var stack:drops)if(stack.is(state.getBlock().asItem()))stack.set(DataComponents.BLOCK_ENTITY_DATA,TypedEntityData.of(entity.getType(),data));return drops;
 }
}
