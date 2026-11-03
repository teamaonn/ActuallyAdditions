package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class FilterScreen extends AbstractContainerScreen<FilterMenu>{
    public FilterScreen(FilterMenu menu,Inventory inv,Component title){super(menu,inv,title,176,173);inventoryLabelY=79;}
    @Override protected void init(){super.init();for(int i=0;i<2;i++){final int id=i;addRenderableWidget(Button.builder(Component.literal(i==0?"Allow / deny":"Match data"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}).bounds(leftPos+8+i*82,topPos+61,78,16).build());}}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){super.extractBackground(g,mouseX,mouseY,delta);g.fill(leftPos,topPos,leftPos+176,topPos+173,0xffd4d4d4);for(var slot:menu.slots)g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff9299a0);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){super.extractLabels(g,x,y);g.text(font,(menu.flags.get(0)==0?"Allow list":"Deny list")+" | Data: "+(menu.flags.get(1)==0?"ignore":"match"),8,17,0xff303740,false);}
}
