package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
public final class ExtraLoot {
 public static void initialize(){net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY_DROPS.register((table,context,drops)->{
  var key=table.unwrapKey();if(key.isEmpty())return;var random=context.getRandom();String id=key.get().identifier().toString();
  if(id.equals("minecraft:entities/bat")&&PortConfig.bool("other","doBatDrops")){
   var attacker=context.getOptional(LootContextParams.ATTACKING_ENTITY);var damage=context.getOptional(LootContextParams.DAMAGE_SOURCE);var victim=context.getOptional(LootContextParams.THIS_ENTITY);if(attacker!=null&&damage!=null&&victim!=null&&net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString().equals("minecraft:bat")){int looting=attacker instanceof LivingEntity living?EnchantmentHelper.getEnchantmentLevel(context.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),living):1;if(random.nextInt(15)<=looting*2)drops.add(new ItemStack(PowerMod.ITEMS.get("bats_wing"),random.nextInt(2+looting)+1));}return;
  }
  if(!PortConfig.bool("other","villageAndDungeonLoot")||!id.startsWith("minecraft:chests/"))return;String chest=id.substring("minecraft:chests/".length());
  boolean crystals=Set.of("simple_dungeon","abandoned_mineshaft","nether_bridge","end_city_treasure","woodland_mansion").contains(chest),core=Set.of("simple_dungeon","abandoned_mineshaft","village/village_weaponsmith","desert_pyramid","nether_bridge","end_city_treasure","woodland_mansion").contains(chest),quartz=Set.of("simple_dungeon","village/village_weaponsmith","end_city_treasure","woodland_mansion").contains(chest),wings=Set.of("stronghold_library","igloo_chest","desert_pyramid","nether_bridge","end_city_treasure","woodland_mansion").contains(chest);
  var names=List.of("restonia","palis","diamatine","void","emeradic","enori");if(crystals){if(random.nextInt(5)==0)drops.add(new ItemStack(PowerMod.ITEMS.get(names.get(random.nextInt(6))+"_crystal"),1+random.nextInt(3)));if(random.nextInt(15)==0)drops.add(new ItemStack(PowerMod.ITEMS.get(names.get(random.nextInt(6))+"_crystal_block"),1+random.nextInt(3)));}
  if(core&&random.nextInt(10)==0)drops.add(new ItemStack(PowerMod.ITEMS.get("drill_core")));if(quartz&&random.nextInt(5)==0)drops.add(new ItemStack(PowerMod.ITEMS.get("black_quartz"),1+random.nextInt(5)));if(wings&&random.nextInt(10)==0)drops.add(new ItemStack(PowerMod.ITEMS.get("bats_wing"),1+random.nextInt(2)));
 });}
}
