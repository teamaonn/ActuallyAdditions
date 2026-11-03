package de.ellpeck.actuallyadditions.fabric.jei;
import java.util.*;
import com.google.gson.*;
import de.ellpeck.actuallyadditions.fabric.content.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import mezz.jei.api.*;
import mezz.jei.api.gui.builder.*;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
@JeiPlugin
public final class ActuallyJei implements IModPlugin {
 private static final Map<String,IRecipeType<Processing.Entry>> TYPES=new LinkedHashMap<>();
 private static final Map<String,String> MACHINES=Map.of("laser","atomic_reconstructor","crushing","crusher","empowering","empowerer","pressing","canola_press","fermenting","fermenting_barrel","liquid_fuel","oil_generator","color_change","lens_of_color","mining_lens","lens_of_the_miner","coffee_ingredient","coffee_machine");
 static{for(var key:MACHINES.keySet().stream().sorted().toList())TYPES.put(key,IRecipeType.create("actuallyadditions",key,Processing.Entry.class));}
 private static IJeiRuntime runtime;private static List<Processing.Entry> shown=List.of();
 public Identifier getPluginUid(){return PowerMod.id("fabric_processing");}
 @Override public void registerCategories(IRecipeCategoryRegistration r){for(var key:TYPES.keySet())r.addRecipeCategories(new Category(key,r.getJeiHelpers().getGuiHelper()));}
 @Override public void registerRecipes(IRecipeRegistration r){shown=Processing.CLIENT;for(var key:TYPES.keySet())r.addRecipes(TYPES.get(key),shown.stream().filter(e->e.type().equals(key)).toList());}
 @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){for(var key:TYPES.keySet())r.addCraftingStation(TYPES.get(key),PowerMod.ITEMS.get(MACHINES.get(key)));r.addCraftingStation(TYPES.get("crushing"),PowerMod.ITEMS.get("crusher_double"));}
 @Override public void onRuntimeAvailable(IJeiRuntime r){runtime=r;refresh();}
 @Override public void onRuntimeUnavailable(){runtime=null;shown=List.of();}
 public static void refresh(){if(runtime==null||shown==Processing.CLIENT)return;var manager=runtime.getRecipeManager();for(var key:TYPES.keySet()){manager.hideRecipes(TYPES.get(key),shown.stream().filter(e->e.type().equals(key)).toList());manager.addRecipes(TYPES.get(key),Processing.CLIENT.stream().filter(e->e.type().equals(key)).toList());}shown=Processing.CLIENT;}
 private static List<ItemStack> ingredients(JsonElement value){
  if(value==null)return List.of();if(value.isJsonArray()){List<ItemStack> result=new ArrayList<>();for(var element:value.getAsJsonArray())result.addAll(ingredients(element));return result;}
  if(value.isJsonObject()){var o=value.getAsJsonObject();if(o.has("base"))return ingredients(o.get("base"));if(o.has("item"))return ingredients(o.get("item"));if(o.has("tag"))return ingredients(new JsonPrimitive("#"+o.get("tag").getAsString()));return List.of();}
  String id=value.getAsString();if(id.startsWith("#")){var set=BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM,Identifier.parse(id.substring(1))));if(set.isEmpty())return List.of();return set.get().stream().map(h->new ItemStack(h.value())).toList();}
  var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id));return item==null?List.of():List.of(new ItemStack(item));
 }
 private static final class Category implements IRecipeCategory<Processing.Entry>{
  private final String key;private final IDrawable icon;Category(String k,IGuiHelper helper){key=k;icon=helper.createDrawableItemLike(PowerMod.ITEMS.get(MACHINES.get(k)));}
  public IRecipeType<Processing.Entry> getRecipeType(){return TYPES.get(key);}public Component getTitle(){return Component.literal(switch(key){case "laser"->"Atomic Reconstructor";case "liquid_fuel"->"Oil Generator Fuel";case "mining_lens"->"Lens of the Miner";case "coffee_ingredient"->"Coffee Ingredients";case "color_change"->"Lens of Color";default->new ItemStack(PowerMod.ITEMS.get(MACHINES.get(key))).getHoverName().getString();});}
  public int getWidth(){return 178;}public int getHeight(){return 100;}public IDrawable getIcon(){return icon;}public Identifier getIdentifier(Processing.Entry e){return e.id();}
  private void fluid(IRecipeLayoutBuilder b,JsonElement json,boolean input,int x){if(json==null)return;var o=json.getAsJsonObject();var fluid=BuiltInRegistries.FLUID.getValue(Identifier.parse(o.get("id").getAsString()));long amount=o.has("amount")?o.get("amount").getAsLong():1000;(input?b.addInputSlot(x,32):b.addOutputSlot(x,32)).add(fluid,amount*81).setFluidRenderer(Math.max(81000,amount*81),false,16,16).setStandardSlotBackground();}
  public void setRecipe(IRecipeLayoutBuilder b,Processing.Entry e,IFocusGroup focus){var d=e.data();var level=Minecraft.getInstance().level;if(level==null)return;
   if(key.equals("fermenting")){fluid(b,d.get("ingredient"),true,8);fluid(b,d.get("result"),false,144);return;}
   if(key.equals("liquid_fuel")){fluid(b,d.get("fuel"),true,8);return;}
   if(key.equals("empowering")){b.addInputSlot(8,8).addItemStacks(ingredients(d.get("base"))).setStandardSlotBackground();int i=0;for(var v:d.getAsJsonArray("modifiers"))b.addInputSlot(8+22*i++,32).addItemStacks(ingredients(v)).setStandardSlotBackground();}
   else b.addInputSlot(8,32).addItemStacks(ingredients(d.get("ingredient"))).setStandardSlotBackground();
   if(key.equals("pressing"))fluid(b,d.get("fluid"),false,144);
   else if(key.equals("coffee_ingredient")){var sample=ingredients(d.get("ingredient"));if(!sample.isEmpty())b.addOutputSlot(144,32).add(Coffee.brew(level,new ArrayList<>(List.of(sample.getFirst().copy())))).setOutputSlotBackground();}
   else if(d.has("result")){if(d.get("result").isJsonArray()){int i=0;for(var part:d.getAsJsonArray("result")){var row=part.getAsJsonObject();double chance=row.has("chance")?row.get("chance").getAsDouble():1;b.addOutputSlot(122+22*i++,32).add(Processing.stack(level,row.get("result"))).setOutputSlotBackground().addRichTooltipCallback((slot,tooltip)->{if(chance<1)tooltip.add(Component.literal("Chance: "+(int)(chance*100)+"%"));});}}else b.addOutputSlot(144,32).add(Processing.stack(level,d.get("result"))).setOutputSlotBackground();}
  }
  public void draw(Processing.Entry e,IRecipeSlotsView slots,GuiGraphicsExtractor g,double x,double y){var font=Minecraft.getInstance().font;g.text(font,"→",103,36,0xff555555,false);int yLine=60;String[] fields={"energy","time","burn_time","total_energy","weight","max_amplifier"};for(var field:fields)if(e.data().has(field)){String label=field.replace('_',' ')+": "+e.data().get(field).getAsString();if(key.equals("empowering")&&field.equals("energy"))label+=" per stand";g.text(font,label,8,yLine,0xff555555,false);yLine+=10;}if(key.equals("crushing"))g.text(font,"40 energy/t • 100 ticks",8,60,0xff555555,false);}
 }
}
