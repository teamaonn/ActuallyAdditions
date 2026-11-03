package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
public final class SackItem extends Item {
    public SackItem(Properties p){super(p.stacksTo(1));}
    @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!l.isClientSide()){var stack=p.getItemInHand(h);if(p.isShiftKeyDown()){EnergyItem.toggle(stack);p.sendOverlayMessage(Component.literal(EnergyItem.enabled(stack)?"Sack collection enabled":"Sack collection disabled"));}else p.openMenu(new SimpleMenuProvider((id,inv,player)->new ItemSlotsMenu(id,inv,stack,28),stack.getHoverName()));}return InteractionResult.SUCCESS;}
    public static int insert(ItemStack sack,ItemStack incoming){
        if(incoming.isEmpty()||incoming.getItem() instanceof SackItem||incoming.getItem() instanceof Accessories.VoidSack)return 0;
        var contents=new ArrayList<>(sack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).itemCopies().toList());while(contents.size()<28)contents.add(ItemStack.EMPTY);int remaining=incoming.getCount();
        for(int pass=0;pass<2&&remaining>0;pass++)for(int i=0;i<28&&remaining>0;i++){var slot=contents.get(i);if(pass==0&&!slot.isEmpty()&&ItemStack.isSameItemSameComponents(slot,incoming)){int n=Math.min(remaining,slot.getMaxStackSize()-slot.getCount());slot.grow(n);remaining-=n;}else if(pass==1&&slot.isEmpty()){int n=Math.min(remaining,incoming.getMaxStackSize());contents.set(i,incoming.copyWithCount(n));remaining-=n;}}
        int moved=incoming.getCount()-remaining;if(moved>0)sack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));return moved;
    }
    @Override public void inventoryTick(ItemStack s,ServerLevel l,Entity e,EquipmentSlot slot){if(!(e instanceof Player p)||p.getMainHandItem()==s||!EnergyItem.enabled(s)||p.containerMenu instanceof ItemSlotsMenu)return;for(var item:l.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(1)))if(item.isAlive()&&!item.hasPickUpDelay()){var stack=item.getItem();int moved=insert(s,stack);if(moved>0){stack.shrink(moved);if(stack.isEmpty())item.discard();else item.setItem(stack);p.getInventory().setChanged();}}}
}
