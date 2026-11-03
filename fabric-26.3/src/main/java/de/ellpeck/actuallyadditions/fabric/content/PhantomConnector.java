package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public final class PhantomConnector extends Item {
    public PhantomConnector(Properties p){super(p.stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getLevel().isClientSide())return InteractionResult.SUCCESS;
        var stack=c.getItemInHand();var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var p=c.getPlayer();
        if(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof WorkEntity e&&e.isPhantom()){
            if(tag.contains("target")&&tag.getStringOr("dimension","").equals(c.getLevel().dimension().identifier().toString())){
                var target=BlockPos.of(tag.getLongOr("target",0));if(target.equals(e.getBlockPos()))return InteractionResult.FAIL;e.bound=target;e.setChanged();if(p!=null)p.sendOverlayMessage(Component.literal("Phantom bound to "+target.toShortString()+" (range "+e.upgradedRange(e.phantomWork()?3:16)+")"));
            }else if(p!=null)p.sendOverlayMessage(Component.literal("Select a target block with the connector first."));
        }else{
            tag.putLong("target",c.getClickedPos().asLong());tag.putString("dimension",c.getLevel().dimension().identifier().toString());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));if(p!=null)p.sendOverlayMessage(Component.literal("Target selected: "+c.getClickedPos().toShortString()));
        }return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!p.isShiftKeyDown())return InteractionResult.PASS;if(!l.isClientSide()){p.getItemInHand(h).remove(DataComponents.CUSTOM_DATA);p.sendOverlayMessage(Component.literal("Phantom target cleared"));}return InteractionResult.SUCCESS;}
}
