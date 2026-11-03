package de.ellpeck.actuallyadditions.fabric.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class EnergyItem extends Item {
    public final int capacity,rate;
    public EnergyItem(int capacity,int rate,Properties properties){super(properties.stacksTo(1));this.capacity=capacity;this.rate=rate;}
    public static int energy(ItemStack stack){return stack.getItem() instanceof EnergyItem item?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("aa_energy",0),0,item.capacity):0;}
    public static void set(ItemStack stack,int value){if(!(stack.getItem() instanceof EnergyItem item))return;var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("aa_energy",Math.clamp(value,0,item.capacity));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static int insert(ItemStack stack,int amount,boolean simulate){if(!(stack.getItem() instanceof EnergyItem item))return 0;int n=Math.max(0,Math.min(Math.min(amount,item.rate),item.capacity-energy(stack)));if(!simulate&&n>0)set(stack,energy(stack)+n);return n;}
    public static int extract(ItemStack stack,int amount,boolean simulate){if(!(stack.getItem() instanceof EnergyItem item))return 0;int n=Math.max(0,Math.min(Math.min(amount,item.rate),energy(stack)));if(!simulate&&n>0)set(stack,energy(stack)-n);return n;}
    public static boolean enabled(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("aa_enabled",false);}
    public static void toggle(ItemStack stack){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putBoolean("aa_enabled",!enabled(stack));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    @Override public boolean isBarVisible(ItemStack stack){return true;}
    @Override public int getBarWidth(ItemStack stack){return (int)(13L*energy(stack)/capacity);}
    @Override public int getBarColor(ItemStack stack){return 0x4FAFEA;}
    @Override public boolean isFoil(ItemStack stack){return enabled(stack);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!player.isShiftKeyDown())return InteractionResult.PASS;
        if(!level.isClientSide()){var stack=player.getItemInHand(hand);toggle(stack);player.sendOverlayMessage(Component.literal(enabled(stack)?"Battery: inventory charging enabled":"Battery: inventory charging disabled"));}return InteractionResult.SUCCESS;
    }
    @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity entity,EquipmentSlot slot){
        if(!(entity instanceof Player player)||!enabled(stack))return;
        int remaining=Math.min(rate,energy(stack));
        for(int i=0;i<player.getInventory().getContainerSize()&&remaining>0;i++){
            var target=player.getInventory().getItem(i);
            // Active batteries do not bounce energy back and forth between themselves.
            if(target==stack||enabled(target))continue;
            int n=insert(target,remaining,false);if(n>0){set(stack,energy(stack)-n);remaining-=n;}
        }
    }
}
