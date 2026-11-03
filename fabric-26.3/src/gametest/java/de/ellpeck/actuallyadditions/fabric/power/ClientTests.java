package de.ellpeck.actuallyadditions.fabric.power;
import de.ellpeck.actuallyadditions.fabric.content.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.world.item.*;
public final class ClientTests implements FabricClientGameTest {
 private void machine(ClientGameTestContext context,TestSingleplayerContext world,WorkKind kind,String screenshot){
  world.getServer().runOnServer(server->{var player=world.getConnection().getServerPlayer();var level=player.level();var pos=player.blockPosition().relative(player.getDirection(),2);level.setBlock(pos,Content.MACHINES.get(kind).defaultBlockState(),3);var machine=(WorkEntity)level.getBlockEntity(pos);machine.getEnergyStorage().setEnergy(Math.min(12345,machine.capacity()));if(kind==WorkKind.DISPLAY)machine.setItem(0,new ItemStack(PowerMod.ITEMS.get("restonia_crystal")));player.openMenu(machine);});
  context.waitForScreen(WorkScreen.class);context.waitTicks(3);context.takeScreenshot(screenshot);
 }
 public void runTest(ClientGameTestContext context){try(var world=context.worldBuilder().create()){
  context.waitFor(client->client.level!=null&&client.player!=null&&!Processing.CLIENT.isEmpty());
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")){context.waitFor(client->de.ellpeck.actuallyadditions.fabric.jei.ActuallyJei.ready());context.runOnClient(client->de.ellpeck.actuallyadditions.fabric.jei.ActuallyJei.validateLayouts());}
  context.runOnClient(client->{for(var item:PowerMod.ITEMS.values()){var state=new net.minecraft.client.renderer.item.ItemStackRenderState();client.getItemModelResolver().updateForTopItem(state,new ItemStack(item),ItemDisplayContext.GUI,client.level,client.player,0);if(state.isEmpty())throw new AssertionError("Item model is empty: "+item);}});
  machine(context,world,WorkKind.DISPLAY,"actuallyadditions-display");machine(context,world,WorkKind.CRUSHER,"actuallyadditions-machine");machine(context,world,WorkKind.FIREWORK,"actuallyadditions-firework");machine(context,world,WorkKind.COFFEE,"actuallyadditions-coffee");
  context.clickScreenButton("Start");world.getConnection().waitForServerboundPackets();world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();if(!(p.containerMenu instanceof WorkMenu))throw new AssertionError("Coffee menu must be a native server menu");});
  context.setScreen(()->new GuideScreen(new GuideMenu(92,net.minecraft.client.Minecraft.getInstance().player.getInventory()),net.minecraft.client.Minecraft.getInstance().player.getInventory(),net.minecraft.network.chat.Component.literal("Actually Additions Manual")));
  context.waitTicks(3);context.takeScreenshot("actuallyadditions-guide");context.setScreen(()->null);
 }}
}
