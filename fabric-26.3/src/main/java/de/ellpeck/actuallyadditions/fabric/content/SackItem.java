package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class SackItem extends Item {
    public SackItem(Properties p){super(p.stacksTo(1));}
    @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!l.isClientSide()){var stack=p.getItemInHand(h);p.openMenu(new SimpleMenuProvider((id,inv,player)->new ItemSlotsMenu(id,inv,stack,28),stack.getHoverName()));}return InteractionResult.SUCCESS;}
}
