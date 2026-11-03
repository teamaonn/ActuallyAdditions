package de.ellpeck.actuallyadditions.fabric.content;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;
public final class UtilityItems {
    public static final TagKey<Block> AIO=TagKey.create(Registries.BLOCK,PowerMod.id("mineable/aio"));
    public static void initialize(){
        ToolMaterial[] materials={ToolMaterial.WOOD,ToolMaterial.STONE,ToolMaterial.IRON,ToolMaterial.GOLD,ToolMaterial.DIAMOND,ToolMaterial.NETHERITE};String[] names={"wooden","stone","iron","gold","diamond","netherite"};
        for(int i=0;i<names.length;i++){var material=materials[i];Content.item(names[i]+"_aiot",Aio::new,new Item.Properties().tool(material,AIO,4,-2F,0).durability(material.durability()*4));}
        Content.item("crafter_on_a_stick",Crafter::new,new Item.Properties().stacksTo(1));
        Content.item("teleport_staff",Teleport::new,new Item.Properties());
    }
    public static final class Aio extends Item {
        Aio(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()==null)return InteractionResult.PASS;if(c.getPlayer().isShiftKeyDown())return Items.IRON_SHOVEL.useOn(c);var result=Items.IRON_AXE.useOn(c);return result.consumesAction()?result:Items.IRON_HOE.useOn(c);}
    }
    public static final class Crafter extends Item {
        Crafter(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!l.isClientSide())p.openMenu(new SimpleMenuProvider((id,inv,player)->new CraftingMenu(id,inv,ContainerLevelAccess.NULL){@Override public boolean stillValid(Player p){return true;}},Component.literal("Crafting")));return InteractionResult.SUCCESS;}
    }
    public static final class Teleport extends EnergyItem {
        Teleport(Properties p){super(250000,1000,p);}
        @Override public InteractionResult use(Level l,Player p,InteractionHand h){
            var stack=p.getItemInHand(h);var hit=p.pick(100,1F,false);var target=BlockPos.containing(hit.getLocation()).above();
            int cost=200+(int)(200*Math.sqrt(p.blockPosition().distManhattan(target)));
            if(energy(stack)<cost)return InteractionResult.FAIL;
            if(l instanceof ServerLevel server){
                if(!server.hasChunkAt(target)||!server.getWorldBorder().isWithinBounds(target))return InteractionResult.FAIL;
                var destination=Vec3.atBottomCenterOf(target);var box=p.getBoundingBox().move(destination.subtract(p.position()));if(!server.noCollision(p,box))return InteractionResult.FAIL;
                p.stopRiding();p.teleportTo(destination.x,destination.y,destination.z);p.resetFallDistance();if(!p.isCreative())set(stack,energy(stack)-cost);p.getCooldowns().addCooldown(stack,50);
                server.playSound(null,target,net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
            }return InteractionResult.SUCCESS;
        }
    }
}
