package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
public final class Coffee {
    public static void initialize(){Content.item("coffee_cup",Item::new,new Item.Properties().stacksTo(1).component(DataComponents.CONSUMABLE,Consumables.DEFAULT_DRINK).usingConvertsTo(PowerMod.ITEMS.get("empty_cup")));}
    public static ItemStack brew(Level level,List<ItemStack> ingredients){
        Map<Holder<MobEffect>,MobEffectInstance> effects=new LinkedHashMap<>();
        for(int i=0;i<ingredients.size();i++){
            var stack=ingredients.get(i);var recipe=Processing.find(level,"coffee_ingredient",stack);if(recipe.isEmpty())continue;boolean used=false;var data=recipe.get().data();
            if(!data.has("effects")){
                if(!effects.isEmpty()){for(var entry:new ArrayList<>(effects.entrySet())){var e=entry.getValue();effects.put(entry.getKey(),new MobEffectInstance(e.getEffect(),e.getDuration()+2400,Math.max(0,e.getAmplifier()-1)));}used=true;}
            }else for(var row:data.getAsJsonArray("effects")){
                var e=row.getAsJsonObject();var holder=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(e.get("effect").getAsString())).orElseThrow();var old=effects.get(holder);int max=Math.max(1,recipe.get().number("max_amplifier",1));
                if(old==null){effects.put(holder,new MobEffectInstance(holder,e.get("duration").getAsInt()*20,e.get("amplifier").getAsInt()));used=true;}
                else if(old.getAmplifier()<max-1){effects.put(holder,new MobEffectInstance(holder,old.getDuration(),old.getAmplifier()+1));used=true;}
            }
            if(used){boolean milk=stack.is(Items.MILK_BUCKET);stack.shrink(1);if(milk)ingredients.set(i,new ItemStack(Items.BUCKET));}
        }
        var result=new ItemStack(PowerMod.ITEMS.get("coffee_cup"));result.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x6b3e23),List.copyOf(effects.values()),Optional.empty()));return result;
    }
}
