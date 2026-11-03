package de.ellpeck.actuallyadditions.fabric.content;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Processing {
    private static final Logger LOG=LoggerFactory.getLogger("actuallyadditions/processing");
    private static final Map<MinecraftServer,List<Entry>> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    public record Entry(Identifier id,String type,JsonObject data){
        public String key(){return id+"#"+data.hashCode();}
        public int number(String name,int fallback){return data.has(name)?Math.max(0,data.get(name).getAsInt()):fallback;}
    }
    public static void initialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(s->reload(s,s.getResourceManager()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s,manager,success)->{if(success)reload(s,manager);});
        ServerLifecycleEvents.SERVER_STOPPED.register(CACHE::remove);
    }
    private static void reload(MinecraftServer server,ResourceManager manager){
        List<Entry> recipes=new ArrayList<>();
        for(var entry:manager.listResources("aa_processing",p->p.getPath().endsWith(".json")).entrySet()){
            try(var reader=entry.getValue().openAsReader()){
                var data=JsonParser.parseReader(reader).getAsJsonObject();
                recipes.add(new Entry(entry.getKey(),data.get("type").getAsString().replace("actuallyadditions:",""),data));
            }catch(Exception ex){throw new IllegalStateException("Invalid Actually Additions processing recipe "+entry.getKey(),ex);}
        }
        recipes.sort(Comparator.comparing(e->e.id().toString()));CACHE.put(server,List.copyOf(recipes));LOG.info("Loaded {} processing recipes",recipes.size());
    }
    public static List<Entry> all(Level level,String type){
        if(!(level instanceof ServerLevel server))return List.of();
        return CACHE.getOrDefault(server.getServer(),List.of()).stream().filter(e->e.type.equals(type)).toList();
    }
    public static Optional<Entry> find(Level level,String type,ItemStack input){return all(level,type).stream().filter(e->matches(e.data.get("ingredient"),input)).findFirst();}
    public static boolean matches(JsonElement definition,ItemStack stack){
        if(definition==null||stack.isEmpty())return false;
        if(definition.isJsonArray()){for(var e:definition.getAsJsonArray())if(matches(e,stack))return true;return false;}
        if(definition.isJsonPrimitive()){
            String text=definition.getAsString();
            return text.startsWith("#")?stack.is(TagKey.create(Registries.ITEM,Identifier.parse(text.substring(1)))):stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(text)));
        }
        var obj=definition.getAsJsonObject();
        if(obj.has("base"))return matches(obj.get("base"),stack);
        if(obj.has("item"))return matches(obj.get("item"),stack);
        if(obj.has("tag"))return stack.is(TagKey.create(Registries.ITEM,Identifier.parse(obj.get("tag").getAsString())));
        return false;
    }
    public static ItemStack stack(Level level,JsonElement definition){
        if(definition==null)return ItemStack.EMPTY;
        var json=definition.deepCopy().getAsJsonObject();
        if(!json.has("count"))json.addProperty("count",1);
        if(!json.has("id"))return ItemStack.EMPTY;
        // Recipe results keep their data components rather than reconstructing bare item IDs.
        return ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),json).getOrThrow();
    }
    public static List<ItemStack> outputs(Level level,Entry recipe,boolean roll){
        List<ItemStack> results=new ArrayList<>();var data=recipe.data();
        if(data.has("result")&&data.get("result").isJsonArray()){
            for(var e:data.getAsJsonArray("result")){
                var part=e.getAsJsonObject();double chance=part.has("chance")?part.get("chance").getAsDouble():1;
                if(!roll||level.getRandom().nextDouble()<chance)results.add(stack(level,part.get("result")));
            }
        }else if(data.has("result"))results.add(stack(level,data.get("result")));
        return results;
    }
    public static int[] assign(JsonArray ingredients,List<ItemStack> inputs){
        if(ingredients.size()!=inputs.size())return null;
        int[] result=new int[inputs.size()];return assignAt(ingredients,inputs,result,0,0)?result:null;
    }
    private static boolean assignAt(JsonArray ingredients,List<ItemStack> inputs,int[] result,int index,int used){
        if(index==ingredients.size())return true;
        for(int i=0;i<inputs.size();i++)if((used&(1<<i))==0&&matches(ingredients.get(index),inputs.get(i))){result[index]=i;if(assignAt(ingredients,inputs,result,index+1,used|(1<<i)))return true;}
        return false;
    }
}
