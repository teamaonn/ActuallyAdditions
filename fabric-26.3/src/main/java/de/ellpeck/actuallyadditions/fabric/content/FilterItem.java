package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
public class FilterItem extends Item {
    public FilterItem(Properties p){super(p.stacksTo(1));}
    public static boolean accepts(ItemStack filter,ItemStack stack){
        if(!(filter.getItem() instanceof FilterItem))return true;
        var data=filter.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();boolean blacklist=data.getBooleanOr("blacklist",false),components=data.getBooleanOr("components",false),any=false,match=false;
        for(var candidate:filter.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList()){any=true;if(candidate.getItem() instanceof Accessories.TagSelector){String tag=candidate.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("item_tag","");if(!tag.isEmpty()&&stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.parse(tag))))match=true;}else if(components?ItemStack.isSameItemSameComponents(candidate,stack):candidate.is(stack.getItem()))match=true;}
        return !any||blacklist!=match;
    }
    @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!l.isClientSide()){var stack=p.getItemInHand(h);p.openMenu(new SimpleMenuProvider((id,inv,player)->new FilterMenu(id,inv,stack),Component.literal("Item Filter")));}return InteractionResult.SUCCESS;}
}
