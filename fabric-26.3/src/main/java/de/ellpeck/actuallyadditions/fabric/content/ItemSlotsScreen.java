package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class ItemSlotsScreen extends AbstractContainerScreen<ItemSlotsMenu>{
    public ItemSlotsScreen(ItemSlotsMenu menu,Inventory inv,Component title){super(menu,inv,title,176,184);inventoryLabelY=90;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){super.extractBackground(g,mouseX,mouseY,delta);g.fill(leftPos,topPos,leftPos+176,topPos+184,0xffd4d4d4);for(var slot:menu.slots)if(slot.isActive())g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff9299a0);}
}
