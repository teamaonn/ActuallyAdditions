package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class WorkScreen extends AbstractContainerScreen<WorkMenu>{
    private net.minecraft.client.gui.components.Button start,priority,mode;
    private final java.util.List<net.minecraft.client.gui.components.Button> settings=new java.util.ArrayList<>();
    private static final String[] NAMES={"Spread","Charges","Flight","Trail","Flicker","Colors","Small","Large","Star","Face","Burst","Area"};
    private boolean relay(){return menu.kind()==WorkKind.ITEM_RELAY||menu.kind()==WorkKind.ADVANCED_ITEM_RELAY||menu.kind()==WorkKind.FLUID_RELAY;}
    @Override protected void init(){super.init();settings.clear();for(int i=0;i<12;i++)for(int j=0;j<2;j++){final int id=100+i*2+j;int x=i<6?8:94,y=28+(i%6)*13;settings.add(addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(j==0?"-":"+"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}).bounds(leftPos+x+(j==0?0:66),topPos+y,9,12).build()));}priority=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Priority +"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,10);}).bounds(leftPos+8,topPos+96,76,16).build());mode=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Mode"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,11);}).bounds(leftPos+92,topPos+96,76,16).build());start=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Start"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+132,topPos+115,36,14).build());}
    @Override protected void containerTick(){super.containerTick();start.visible=menu.kind()==WorkKind.COFFEE;priority.visible=mode.visible=relay();for(var b:settings)b.visible=menu.kind()==WorkKind.FIREWORK;}
    public WorkScreen(WorkMenu menu,Inventory inv,Component title){super(menu,inv,title,176,240);inventoryLabelY=144;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractBackground(g,mouseX,mouseY,delta);int x=leftPos,y=topPos;
        g.fill(x,y,x+176,y+240,0xff30363d);g.fill(x+2,y+2,x+174,y+238,0xffd4d4d4);
        for(var slot:menu.slots){if(!slot.isActive())continue;g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xff454c55);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xff9299a0);}
        g.fill(x+10,y+130,x+166,y+138,0xff454c55);if(menu.capacity()>0)g.fill(x+11,y+131,x+11+(int)(154L*menu.energy()/menu.capacity()),y+137,0xffdca735);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){
        super.extractLabels(g,x,y);String label=switch(menu.kind()){case RECONSTRUCTOR->menu.data.get(5)==1?"Pulse mode":"Automatic: every 5s";case EMPOWERER->"Base + four stands 3 blocks away";case DISPLAY->"Display or empowering ingredient";case ITEM_RELAY,ADVANCED_ITEM_RELAY,FLUID_RELAY->"Link with the laser wrench";case ITEM_INTERFACE,HOPPING_INTERFACE->"Connect to an item relay network";case FIREWORK->"Configure fireworks below";default->"Inputs left; outputs right";};
        g.text(font,label,8,21,0xff303740,false);if(menu.capacity()>0)g.text(font,menu.energy()+" / "+menu.capacity()+" energy",8,120,0xff303740,false);
        if(relay())g.text(font,"Priority "+menu.data.get(14)+" | "+new String[]{"Both","Input","Output"}[Math.clamp(menu.data.get(15),0,2)],8,82,0xff303740,false);
        if(menu.kind()==WorkKind.FIREWORK)g.text(font,"Effects and shapes use %",8,110,0xff303740,false);if(menu.kind()==WorkKind.FIREWORK)for(int i=0;i<12;i++)g.text(font,NAMES[i]+": "+menu.data.get(16+i),i<6?18:104,30+(i%6)*13,0xff303740,false);
        if(menu.kind()==WorkKind.COFFEE||menu.kind()==WorkKind.PRESS||menu.kind()==WorkKind.FERMENT||menu.kind()==WorkKind.OIL||menu.kind()==WorkKind.FLUID_PLACER||menu.kind()==WorkKind.FLUID_COLLECTOR)g.text(font,"Tanks: "+menu.data.get(9)+" / "+menu.data.get(10)+" mB",8,110,0xff303740,false);
        if(menu.kind()==WorkKind.CRUSHER||menu.kind()==WorkKind.DOUBLE_CRUSHER||menu.kind()==WorkKind.FURNACE)g.text(font,"Progress: "+menu.data.get(2)+" / "+menu.data.get(3),8,110,0xff303740,false);
    }
}
