package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
public final class SolidExperience extends Item {
    public SolidExperience(Properties p){super(p);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide()){var stack=player.getItemInHand(hand);int n=player.isShiftKeyDown()?stack.getCount():1;if(PortConfig.bool("other","solidXPOrbs")&&level instanceof net.minecraft.server.level.ServerLevel server)net.minecraft.world.entity.ExperienceOrb.award(server,player.position(),n*8);else player.giveExperiencePoints(n*8);if(!player.isCreative())stack.shrink(n);}return InteractionResult.SUCCESS;
    }
}
