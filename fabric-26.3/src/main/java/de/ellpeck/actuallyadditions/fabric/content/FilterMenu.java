package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import java.util.*;
/** Ghost slots copy item/components without taking items out of the player's inventory. */
public final class FilterMenu extends AbstractContainerMenu {
    private final ItemStack filter;private final Inventory playerInventory;private final SimpleContainer ghosts=new SimpleContainer(18);
    public final ContainerData flags;
    public FilterMenu(int id,Inventory inv){this(id,inv,ItemStack.EMPTY);}
    public FilterMenu(int id,Inventory inv,ItemStack stack){
        super(Content.FILTER_MENU,id);filter=stack;playerInventory=inv;
        var content=stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).itemCopies().toList();for(int i=0;i<Math.min(18,content.size());i++)ghosts.setItem(i,content.get(i).copy());
        flags=new SimpleContainerData(2);var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();flags.set(0,tag.getBooleanOr("blacklist",false)?1:0);flags.set(1,tag.getBooleanOr("components",false)?1:0);addDataSlots(flags);
        for(int r=0;r<2;r++)for(int c=0;c<9;c++)addSlot(new Slot(ghosts,r*9+c,8+c*18,28+r*18){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,9+r*9+c,8+c*18,91+r*18));for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,149));
    }
    private void save(){if(filter.isEmpty())return;List<ItemStack> contents=new ArrayList<>();for(int i=0;i<18;i++)contents.add(ghosts.getItem(i));filter.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));var tag=filter.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putBoolean("blacklist",flags.get(0)!=0);tag.putBoolean("components",flags.get(1)!=0);filter.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));playerInventory.setChanged();}
    @Override public void clicked(int index,int button,ContainerInput type,Player player){
        if(index>=0&&index<18){if(type==ContainerInput.PICKUP){ghosts.setItem(index,getCarried().isEmpty()?ItemStack.EMPTY:getCarried().copyWithCount(1));save();}return;}
        super.clicked(index,button,type,player);
    }
    @Override public boolean clickMenuButton(Player p,int id){if(id<0||id>1)return false;flags.set(id,1-flags.get(id));save();return true;}
    @Override public boolean stillValid(Player p){return filter.isEmpty()||playerInventory.contains(filter);}
    @Override public ItemStack quickMoveStack(Player p,int index){if(index<18||index>=slots.size())return ItemStack.EMPTY;var stack=slots.get(index).getItem();if(stack.isEmpty())return ItemStack.EMPTY;for(int i=0;i<18;i++)if(ghosts.getItem(i).isEmpty()){ghosts.setItem(i,stack.copyWithCount(1));save();break;}return ItemStack.EMPTY;}
}
