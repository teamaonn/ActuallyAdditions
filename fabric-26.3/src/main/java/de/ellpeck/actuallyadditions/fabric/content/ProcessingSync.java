package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import com.google.gson.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.*;
public record ProcessingSync(String json) implements CustomPacketPayload {
 public static final Type<ProcessingSync> TYPE=new Type<>(PowerMod.id("processing_recipes"));
 public static final StreamCodec<RegistryFriendlyByteBuf,ProcessingSync> CODEC=StreamCodec.of((buf,p)->buf.writeUtf(p.json,1048576),buf->new ProcessingSync(buf.readUtf(1048576)));
 @Override public Type<ProcessingSync> type(){return TYPE;}
 public static void initialize(){PayloadTypeRegistry.clientboundPlay().registerLarge(TYPE,CODEC,4194304);ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->send(server,handler.player));}
 public static void send(MinecraftServer server,ServerPlayer player){if(ServerPlayNetworking.canSend(player,TYPE)){var list=new JsonArray();for(var e:Processing.snapshot(server)){var row=new JsonObject();row.addProperty("id",e.id().toString());row.addProperty("type",e.type());row.add("data",e.data());list.add(row);}ServerPlayNetworking.send(player,new ProcessingSync(list.toString()));}}
 public List<Processing.Entry> recipes(){List<Processing.Entry> result=new ArrayList<>();for(var row:JsonParser.parseString(json).getAsJsonArray()){var data=row.getAsJsonObject();result.add(new Processing.Entry(Identifier.parse(data.get("id").getAsString()),data.get("type").getAsString(),data.getAsJsonObject("data")));}return List.copyOf(result);}
}
