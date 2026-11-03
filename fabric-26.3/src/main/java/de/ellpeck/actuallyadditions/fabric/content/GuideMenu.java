package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
public final class GuideMenu extends AbstractContainerMenu {
 public GuideMenu(int id,Inventory inv){super(Content.GUIDE_MENU,id);}
 @Override public boolean stillValid(Player p){return true;}
 @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
