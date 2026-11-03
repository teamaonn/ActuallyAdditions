package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
public final class Lenses extends Item {
    public final String kind;public final int distance,minimum;
    public Lenses(String kind,int distance,int minimum,Properties p){super(p.stacksTo(1));this.kind=kind;this.distance=distance;this.minimum=minimum;}
    public static void initialize(){
        Content.item("lens_of_color",p->new Lenses("color",10,200,p),new Item.Properties());
        Content.item("lens_of_the_miner",p->new Lenses("mining",10,60000,p),new Item.Properties());
        Content.item("lens_of_certain_death",p->new Lenses("death",15,350,p),new Item.Properties());
        Content.item("lens_of_the_killer",p->new Lenses("killer",15,2500,p),new Item.Properties());
        Content.item("lens_of_detonation",p->new Lenses("detonation",30,250000,p),new Item.Properties());
        Content.item("lens_of_disenchanting",p->new Lenses("disenchant",5,250000,p),new Item.Properties());
    }
    public void fire(WorkEntity owner,ServerLevel level,BlockPos end,AABB beam){
        switch(kind){
            case "death","killer" -> {var fake=net.fabricmc.fabric.api.entity.FakePlayer.get(level);for(var entity:level.getEntitiesOfClass(LivingEntity.class,beam))if(owner.spend(minimum))entity.hurtServer(level,kind.equals("killer")?level.damageSources().playerAttack(fake):level.damageSources().magic(),20);}
            case "detonation" -> {if(owner.spend(250000))level.explode(null,end.getX()+.5,end.getY()+.5,end.getZ()+.5,10F,true,Level.ExplosionInteraction.NONE);}
            case "mining" -> {
                if(level.getBlockEntity(end)!=null)return;var input=new ItemStack(level.getBlockState(end).getBlock());var recipes=Processing.all(level,"mining_lens").stream().filter(r->Processing.matches(r.data().get("ingredient"),input)).toList();int total=recipes.stream().mapToInt(r->r.number("weight",0)).sum();if(total<=0||owner.energy()<60000)return;int roll=level.getRandom().nextInt(total);
                for(var recipe:recipes){roll-=recipe.number("weight",0);if(roll>=0)continue;var result=Processing.stack(level,recipe.data().get("result"));if(result.getItem() instanceof BlockItem block&&level.setBlockAndUpdate(end,block.getBlock().defaultBlockState()))owner.spend(60000);break;}
            }
            case "color" -> {
                if(level.getBlockEntity(end)==null){var recipe=Processing.find(level,"color_change",new ItemStack(level.getBlockState(end).getBlock()));if(recipe.isPresent()&&owner.energy()>=200){var result=Processing.stack(level,recipe.get().data().get("result"));if(result.getItem() instanceof BlockItem block&&level.setBlockAndUpdate(end,block.getBlock().defaultBlockState()))owner.spend(200);}}
                for(var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(end))){var recipe=Processing.find(level,"color_change",entity.getItem());if(recipe.isEmpty()||owner.energy()<200)continue;var result=Processing.stack(level,recipe.get().data().get("result"));if(!result.isEmpty()){result.setCount(entity.getItem().getCount());entity.setItem(result);owner.spend(200);}}
            }
            case "disenchant" -> {
                var entities=level.getEntitiesOfClass(ItemEntity.class,new AABB(end));ItemEntity book=null,tool=null;
                for(var entity:entities){var stack=entity.getItem();if(stack.getCount()!=1)continue;if(stack.is(Items.BOOK)||stack.is(Items.ENCHANTED_BOOK)){if(book!=null)return;book=entity;}else if(!stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).isEmpty()){if(tool!=null)return;tool=entity;}}
                if(book==null||tool==null||owner.energy()<250000)return;var enchantments=tool.getItem().getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);var enchant=enchantments.keySet().iterator().next();int rank=enchantments.getLevel(enchant);
                var updatedTool=tool.getItem().copy();var remaining=new ItemEnchantments.Mutable(enchantments);remaining.set(enchant,0);updatedTool.set(DataComponents.ENCHANTMENTS,remaining.toImmutable());
                var updatedBook=book.getItem().is(Items.ENCHANTED_BOOK)?book.getItem().copy():new ItemStack(Items.ENCHANTED_BOOK);var stored=new ItemEnchantments.Mutable(updatedBook.getOrDefault(DataComponents.STORED_ENCHANTMENTS,ItemEnchantments.EMPTY));stored.upgrade(enchant,rank);updatedBook.set(DataComponents.STORED_ENCHANTMENTS,stored.toImmutable());
                tool.setItem(updatedTool);book.setItem(updatedBook);owner.spend(250000);
            }
        }
    }
}
