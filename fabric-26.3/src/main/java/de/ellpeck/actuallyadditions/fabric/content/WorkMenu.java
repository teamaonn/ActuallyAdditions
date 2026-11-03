package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class WorkMenu extends AbstractContainerMenu {
    public final ContainerData data;private final Container container;
    public WorkMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(12),new SimpleContainerData(14));}
    public WorkMenu(int id,Inventory inventory,Container container,ContainerData data){
        super(Content.MENU,id);this.container=container;this.data=data;
        for(int i=0;i<12;i++){final int slot=i;addSlot(new Slot(container,i,35+(i%3)*36,32+(i/3)*20){
            @Override public boolean isActive(){return slot<kind().slots;}
            @Override public boolean mayPlace(ItemStack s){return container.canPlaceItem(slot,s);}
            @Override public int getMaxStackSize(){return kind()==WorkKind.DISPLAY||kind()==WorkKind.EMPOWERER?1:64;}
        });}
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inventory,c+r*9+9,8+c*18,156+r*18));
        for(int c=0;c<9;c++)addSlot(new Slot(inventory,c,8+c*18,214));addDataSlots(data);
    }
    public WorkKind kind(){return WorkKind.values()[Math.clamp(data.get(4),0,WorkKind.values().length-1)];}
    public int capacity(){return (data.get(7)&65535)|((data.get(8)&65535)<<16);}
    public int energy(){return (data.get(0)&65535)|((data.get(1)&65535)<<16);}
    @Override public boolean clickMenuButton(Player player,int id){if(id==0&&container instanceof WorkEntity e&&e.kind()==WorkKind.COFFEE){e.brewing=true;e.setChanged();return true;}return false;}
    @Override public boolean stillValid(Player player){return container.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<12){if(!moveItemStackTo(stack,12,slots.size(),true))return ItemStack.EMPTY;}
        else{boolean moved=false;for(int i=0;i<6&&!stack.isEmpty();i++)if(slots.get(i).mayPlace(stack))moved=moveItemStackTo(stack,i,i+1,false)||moved;if(!moved)return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
