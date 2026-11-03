package de.ellpeck.actuallyadditions.fabric.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

public final class LaserWrench extends Item {
    public LaserWrench(Properties props) { super(props); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var pos = context.getClickedPos();
        if(player!=null&&level.getBlockEntity(pos) instanceof de.ellpeck.actuallyadditions.fabric.content.WorkEntity relay&&relay.isTransportRelay())return useTransport(context,relay);
        if (player == null || !(level.getBlockEntity(pos) instanceof RelayEntity target)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        var stack = context.getItemInHand();
        if (player.isShiftKeyDown()) { target.unlinkAll(); clear(stack); message(player, "All connections removed from this relay."); return InteractionResult.SUCCESS; }
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.contains("relay_dimension")) {
            String dimension = data.getStringOr("relay_dimension", "");
            var firstPos = new BlockPos(data.getIntOr("relay_x", 0), data.getIntOr("relay_y", 0), data.getIntOr("relay_z", 0));
            if (!dimension.equals(level.dimension().identifier().toString())) { clear(stack); message(player, "Choose two relays in the same dimension."); return InteractionResult.SUCCESS; }
            if (!level.hasChunkAt(firstPos)) { message(player, "The first relay is in an unloaded chunk."); return InteractionResult.SUCCESS; }
            if (!(level.getBlockEntity(firstPos) instanceof RelayEntity first) || !first.node.toString().equals(data.getStringOr("relay_node", ""))) {
                clear(stack); message(player, "The first relay was removed or replaced. Select it again."); return InteractionResult.SUCCESS;
            }
            if (!level.mayInteract(player, firstPos)) return InteractionResult.FAIL;
            message(player, RelayEntity.connect(first, target)); clear(stack);
        } else {
            data.putString("relay_dimension", level.dimension().identifier().toString()); data.putString("relay_node", target.node.toString());
            data.putInt("relay_x", pos.getX()); data.putInt("relay_y", pos.getY()); data.putInt("relay_z", pos.getZ());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            message(player, "First relay selected. Right-click another relay to connect; repeat to disconnect.");
        }
        return InteractionResult.SUCCESS;
    }
    private InteractionResult useTransport(UseOnContext c,de.ellpeck.actuallyadditions.fabric.content.WorkEntity target){
        var l=c.getLevel();var p=c.getPlayer();var stack=c.getItemInHand();var pos=c.getClickedPos();if(l.isClientSide())return InteractionResult.SUCCESS;if(!l.mayInteract(p,pos)||!p.mayUseItemAt(pos,c.getClickedFace(),stack))return InteractionResult.FAIL;
        if(p.isShiftKeyDown()){target.network.unlink();clear(stack);message(p,"Relay connections removed.");return InteractionResult.SUCCESS;}var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(data.contains("relay_dimension")){var firstPos=new BlockPos(data.getIntOr("relay_x",0),data.getIntOr("relay_y",0),data.getIntOr("relay_z",0));if(!data.getStringOr("relay_dimension","").equals(l.dimension().identifier().toString())||!l.hasChunkAt(firstPos)){message(p,"Select loaded relays in the same dimension.");return InteractionResult.SUCCESS;}
            if(l.getBlockEntity(firstPos) instanceof de.ellpeck.actuallyadditions.fabric.content.WorkEntity first&&first.isTransportRelay()&&first.network.id.toString().equals(data.getStringOr("relay_node","")))message(p,de.ellpeck.actuallyadditions.fabric.content.Transport.connect(first.network,target.network));else message(p,"The first relay changed or has another resource type.");clear(stack);
        }else{data.putString("relay_dimension",l.dimension().identifier().toString());data.putString("relay_node",target.network.id.toString());data.putInt("relay_x",pos.getX());data.putInt("relay_y",pos.getY());data.putInt("relay_z",pos.getZ());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));message(p,"First relay selected; select another to connect.");}return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) { if (!level.isClientSide()) { clear(player.getItemInHand(hand)); message(player, "Relay selection cleared."); } return InteractionResult.SUCCESS; }
        return InteractionResult.PASS;
    }
    private static void clear(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (var key : new String[]{"relay_dimension", "relay_node", "relay_x", "relay_y", "relay_z"}) data.remove(key);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
    private static void message(Player player, String text) { player.sendOverlayMessage(Component.literal(text)); }
}
