package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import com.google.gson.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
public final class GuideScreen extends AbstractContainerScreen<GuideMenu> {
 private record Chapter(String title,String text){}private record Page(String title,List<FormattedCharSequence> lines){}
 private final List<Chapter> chapters=new ArrayList<>();private final List<Page> pages=new ArrayList<>();private int page;
 public GuideScreen(GuideMenu m,Inventory inv,Component title){super(m,inv,title,320,240);}
 private void search(String q){pages.clear();page=0;for(var c:chapters)if((c.title+" "+c.text).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))){var lines=font.split(Component.literal(c.text),300);for(int i=0;i<lines.size();i+=16)pages.add(new Page(c.title,List.copyOf(lines.subList(i,Math.min(lines.size(),i+16)))));}}
 @Override protected void init(){super.init();chapters.clear();try(var reader=minecraft.getResourceManager().openAsReader(PowerMod.id("guide.json"))){for(var row:JsonParser.parseReader(reader).getAsJsonArray()){var c=row.getAsJsonObject();chapters.add(new Chapter(c.get("title").getAsString(),c.get("text").getAsString()));}}catch(Exception e){chapters.add(new Chapter("Manual",e.toString()));}search("");var input=new EditBox(font,leftPos+8,topPos+26,304,18,Component.literal("Search manual"));input.setHint(Component.literal("Search manual"));input.setResponder(this::search);addRenderableWidget(input);addRenderableWidget(Button.builder(Component.literal("Previous"),b->{if(page>0)page--;}).bounds(leftPos+8,topPos+215,70,18).build());addRenderableWidget(Button.builder(Component.literal("Next"),b->{if(page+1<pages.size())page++;}).bounds(leftPos+242,topPos+215,70,18).build());}
 @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float d){super.extractBackground(g,x,y,d);g.fill(leftPos,topPos,leftPos+320,topPos+240,0xff61472f);g.fill(leftPos+2,topPos+2,leftPos+318,topPos+238,0xffead9b5);}
 @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){g.text(font,"Actually Additions Manual",8,8,0xff382b20,false);if(pages.isEmpty()){g.text(font,"No matching chapters",8,54,0xff382b20,false);return;}var p=pages.get(Math.min(page,pages.size()-1));g.text(font,p.title,8,51,0xff382b20,false);for(int i=0;i<p.lines.size();i++)g.text(font,p.lines.get(i),8,66+i*9,0xff382b20,false);g.text(font,(page+1)+" / "+pages.size(),135,220,0xff382b20,false);}
}
