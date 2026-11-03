package de.ellpeck.actuallyadditions.fabric.content;
import java.io.*;
import java.nio.file.*;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
public final class PortConfig {
 private static final JsonObject DATA=new JsonObject();
 static {
  var machines=new JsonObject();machines.addProperty("farmerArea",9);machines.addProperty("reconstructorPower",300000);machines.addProperty("oilGeneratorTransfer",500);machines.addProperty("minerLensEnergy",60000);machines.addProperty("laserRelayLoss",true);machines.addProperty("leafGeneratorCooldown",5);machines.addProperty("leafGeneratorCPPerLeaf",300);machines.addProperty("leafGeneratorArea",7);machines.addProperty("fluidLaserTransferRate",10000);machines.add("verticalDiggerExtraWhitelist",new JsonArray());machines.add("verticalDiggerBlacklist",new JsonArray());DATA.add("machineSettings",machines);
  var worldgen=new JsonObject();for(var s:new String[]{"generateQuartz","generateCanola","generateFlax","generateCoffee","generateRice"})worldgen.addProperty(s,true);DATA.add("worldgenSettings",worldgen);
  var other=new JsonObject();for(var s:new String[]{"doBatDrops","tillingWorms","villageAndDungeonLoot","waterBowl","waterBowlSpilling"})other.addProperty(s,true);other.addProperty("wormDeathTime",0);other.addProperty("solidXPOrbs",false);DATA.add("other",other);
  var items=new JsonObject();items.add("drillExtraWhitelist",new JsonArray());DATA.add("itemsSettings",items);
 }
 public static void load(){var path=FabricLoader.getInstance().getConfigDir().resolve("actuallyadditions.json");try{if(Files.exists(path)){var saved=JsonParser.parseString(Files.readString(path)).getAsJsonObject();for(var section:DATA.entrySet())if(saved.has(section.getKey()))for(var entry:section.getValue().getAsJsonObject().entrySet()){var value=saved.getAsJsonObject(section.getKey()).get(entry.getKey());if(value!=null)entry.setValue(value);}}else{Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(DATA)+"\n");}}catch(Exception ex){throw new IllegalStateException("Cannot read Actually Additions config "+path,ex);}}
 public static boolean bool(String section,String key){return DATA.getAsJsonObject(section).get(key).getAsBoolean();}
 public static int number(String key,int min,int max){return Math.clamp(DATA.getAsJsonObject("machineSettings").get(key).getAsInt(),min,max);}
 public static int area(String key){return number(key,1,65)|1;}
 public static int wormLife(){return Math.clamp(DATA.getAsJsonObject("other").get("wormDeathTime").getAsInt(),0,10000000);}
 public static boolean contains(String section,String key,String id){for(var e:DATA.getAsJsonObject(section).getAsJsonArray(key))if(e.getAsString().equals(id))return true;return false;}
}
