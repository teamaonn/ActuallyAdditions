package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
/** A real item inventory; the owner item stays locked while its contents are open. */
public final class ItemSlotsMenu extends AbstractContainerMenu {
    private final ItemStack owner;private final Inventory inventory;private final SimpleContainer contents;
    public final ContainerData data=new SimpleContainerData(1);
    public ItemSlotsMenu(int id,Inventory inv){this(id,inv,ItemStack.EMPTY,28);}
    public ItemSlotsMenu(int id,Inventory inv,ItemStack owner,int size){
        super(Content.ITEM_MENU,id);this.owner=owner;inventory=inv;data.set(0,size);addDataSlots(data);
        contents=new SimpleContainer(28){@Override public void setChanged(){super.setChanged();save();}};
        var saved=owner.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).itemCopies().toList();for(int i=0;i<Math.min(size,saved.size());i++)contents.setItem(i,saved.get(i));
        for(int i=0;i<28;i++){final int index=i;addSlot(new Slot(contents,i,8+(i%9)*18,18+(i/9)*18){@Override public boolean isActive(){return index<data.get(0);}@Override public boolean mayPlace(ItemStack stack){return index<data.get(0)&&allowed(stack);}@Override public int getMaxStackSize(){return owner.getItem() instanceof DrillItem?1:64;}});}
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addPlayerSlot(inv,9+r*9+c,8+c*18,102+r*18);for(int c=0;c<9;c++)addPlayerSlot(inv,c,8+c*18,160);
    }
    private void addPlayerSlot(Inventory inv,int i,int x,int y){addSlot(new Slot(inv,i,x,y){@Override public boolean mayPickup(Player p){return getItem()!=owner;}@Override public boolean mayPlace(ItemStack stack){return getItem()!=owner;}});}
    private boolean allowed(ItemStack stack){return owner.getItem() instanceof DrillItem?stack.getItem() instanceof DrillItem.Upgrade:!(stack.getItem() instanceof SackItem)&&!(stack.getItem() instanceof Accessories.VoidSack)&&stack!=owner;}
    private void save(){if(owner.isEmpty()||contents==null)return;var list=new ArrayList<ItemStack>();for(int i=0;i<data.get(0);i++)list.add(contents.getItem(i));owner.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(list));inventory.setChanged();}
    @Override public boolean stillValid(Player p){if(owner.isEmpty())return true;for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i)==owner)return true;return false;}
    @Override public void clicked(int i,int button,ContainerInput type,Player p){if(i>=28&&i<slots.size()&&slots.get(i).getItem()==owner)return;if(type==ContainerInput.SWAP&&button>=0&&button<inventory.getContainerSize()&&inventory.getItem(button)==owner)return;super.clicked(i,button,type,p);}
    @Override public ItemStack quickMoveStack(Player p,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty()||stack==owner)return ItemStack.EMPTY;var copy=stack.copy();if(index<28){if(!moveItemStackTo(stack,28,slots.size(),true))return ItemStack.EMPTY;}else if(!allowed(stack)||!moveItemStackTo(stack,0,data.get(0),false))return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);save();return copy;}
}
