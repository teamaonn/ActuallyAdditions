package de.ellpeck.actuallyadditions.fabric.content;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
public final class ClientRecipes {
 public static void initialize(){
  ClientPlayNetworking.registerGlobalReceiver(ProcessingSync.TYPE,(payload,context)->{Processing.CLIENT=payload.recipes();if(FabricLoader.getInstance().isModLoaded("jei"))de.ellpeck.actuallyadditions.fabric.jei.ActuallyJei.refresh();});
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{Processing.CLIENT=List.of();if(FabricLoader.getInstance().isModLoaded("jei"))de.ellpeck.actuallyadditions.fabric.jei.ActuallyJei.refresh();});
 }
}
