package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class WorkScreen extends AbstractContainerScreen<WorkMenu>{
    public WorkScreen(WorkMenu menu,Inventory inv,Component title){super(menu,inv,title,176,216);inventoryLabelY=120;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractBackground(g,mouseX,mouseY,delta);int x=leftPos,y=topPos;
        g.fill(x,y,x+176,y+216,0xff30363d);g.fill(x+2,y+2,x+174,y+214,0xffd4d4d4);
        for(var slot:menu.slots){g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xff454c55);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xff9299a0);}
        g.fill(x+10,y+106,x+166,y+114,0xff454c55);if(menu.capacity()>0)g.fill(x+11,y+107,x+11+(int)(154L*menu.energy()/menu.capacity()),y+113,0xffdca735);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){
        super.extractLabels(g,x,y);String label=switch(menu.kind()){case RECONSTRUCTOR->menu.data.get(5)==1?"Pulse mode":"Automatic: every 5s";case EMPOWERER->"Base + four stands 3 blocks away";case DISPLAY->"Empowering ingredient";default->"Inputs left; outputs right";};
        g.text(font,label,8,21,0xff303740,false);g.text(font,menu.energy()+" / "+menu.capacity()+" energy",8,96,0xff303740,false);
        if(menu.kind()==WorkKind.CRUSHER||menu.kind()==WorkKind.DOUBLE_CRUSHER||menu.kind()==WorkKind.FURNACE)g.text(font,"Progress: "+menu.data.get(2)+" / "+menu.data.get(3),8,86,0xff303740,false);
    }
}
