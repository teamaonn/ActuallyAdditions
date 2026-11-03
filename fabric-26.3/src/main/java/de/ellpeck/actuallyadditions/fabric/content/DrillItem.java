package de.ellpeck.actuallyadditions.fabric.content;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;
public final class DrillItem extends EnergyItem {
    private static final Set<UUID> BREAKING=new HashSet<>();
    public static final TagKey<Block> MINEABLE=TagKey.create(Registries.BLOCK,PowerMod.id("mineable/drill"));
    public static final List<String> UPGRADES=List.of("speed","speed_ii","speed_iii","silk_touch","fortune","fortune_ii","three_by_three","five_by_five","block_placing");
    public DrillItem(Properties p){super(250000,1000,p.tool(ToolMaterial.NETHERITE,MINEABLE,8,-3,0));}
    public static boolean has(ItemStack drill,String upgrade){return drill.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().anyMatch(s->s.is(PowerMod.ITEMS.get("drill_upgrade_"+upgrade)));}
    public static int cost(ItemStack s){int n=100;if(has(s,"speed")){n+=50;if(has(s,"speed_ii")){n+=75;if(has(s,"speed_iii"))n+=175;}}if(has(s,"silk_touch"))n+=100;if(has(s,"fortune")){n+=40;if(has(s,"fortune_ii"))n+=80;}if(has(s,"three_by_three")){n+=10;if(has(s,"five_by_five"))n+=30;}return n;}
    private static boolean mineable(BlockState state){return state.is(MINEABLE)||PortConfig.contains("itemsSettings","drillExtraWhitelist",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());}
    @Override public float getDestroySpeed(ItemStack s,BlockState state){if(energy(s)<cost(s))return .1F;if(!mineable(state))return 1;float n=8;if(has(s,"speed"))n+=has(s,"speed_ii")?(has(s,"speed_iii")?37:25):8;if(has(s,"three_by_three")){n*=.5F;if(has(s,"five_by_five"))n*=.35F;}return n;}
    @Override public boolean isCorrectToolForDrops(ItemStack s,BlockState state){return energy(s)>=cost(s)&&(super.isCorrectToolForDrops(s,state)||PortConfig.contains("itemsSettings","drillExtraWhitelist",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));}
    @Override public boolean mineBlock(ItemStack s,Level l,BlockState state,BlockPos pos,LivingEntity entity){if(!l.isClientSide()&&state.getDestroySpeed(l,pos)>0)set(s,energy(s)-cost(s));return true;}
    @Override public void hurtEnemy(ItemStack s,LivingEntity target,LivingEntity attacker){set(s,energy(s)-100);}
    @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!l.isClientSide()){var stack=p.getItemInHand(h);p.openMenu(new SimpleMenuProvider((id,inv,player)->new ItemSlotsMenu(id,inv,stack,9),Component.literal("Drill upgrades")));}return InteractionResult.SUCCESS;}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null||!has(c.getItemInHand(),"block_placing"))return InteractionResult.PASS;
        for(var upgrade:c.getItemInHand().getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList())if(upgrade.is(PowerMod.ITEMS.get("drill_upgrade_block_placing"))){
            int slot=upgrade.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("slot",0);var place=c.getPlayer().getInventory().getItem(Math.clamp(slot,0,8));
            if(place==c.getItemInHand()||place.isEmpty())return InteractionResult.PASS;return place.useOn(new UseOnContext(c.getLevel(),c.getPlayer(),c.getHand(),place,new BlockHitResult(c.getClickLocation(),c.getClickedFace(),c.getClickedPos(),c.isInside())));
        }return InteractionResult.PASS;
    }
    public static void initialize(){
        for(var color:DyeColor.values())Content.item("drill_"+color.getName(),DrillItem::new,new Item.Properties());
        for(var upgrade:UPGRADES)Content.item("drill_upgrade_"+upgrade,Upgrade::new,new Item.Properties().stacksTo(1));
        Content.item("travelers_sack",SackItem::new,new Item.Properties());
        PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity)->{
            var stack=player.getMainHandItem();if(!(stack.getItem() instanceof DrillItem)||!(player instanceof ServerPlayer server))return true;
            if(BREAKING.contains(player.getUUID()))return true;if(energy(stack)<cost(stack))return false;
            BREAKING.add(player.getUUID());var original=stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
            try{
                if(has(stack,"silk_touch"))stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);else if(has(stack,"fortune"))stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),has(stack,"fortune_ii")?3:1);
                int radius=player.isShiftKeyDown()||!has(stack,"three_by_three")?0:has(stack,"five_by_five")?2:1;
                var hit=player.pick(player.blockInteractionRange(),1F,false);var side=hit instanceof BlockHitResult b?b.getDirection():Direction.UP;
                float hardness=state.getDestroySpeed(level,pos);server.gameMode.destroyBlock(pos);
                for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++){
                    if(a==0&&b==0)continue;if(energy(stack)<cost(stack))return false;
                    var target=switch(side.getAxis()){case X->pos.offset(0,a,b);case Y->pos.offset(a,0,b);case Z->pos.offset(a,b,0);};if(!level.hasChunkAt(target))continue;
                    var block=level.getBlockState(target);if(!mineable(block)||block.getDestroySpeed(level,target)<0||block.getDestroySpeed(level,target)>hardness+5)continue;
                    server.gameMode.destroyBlock(target);
                }
            }finally{stack.set(DataComponents.ENCHANTMENTS,original);BREAKING.remove(player.getUUID());}return false;
        });
    }
    public static final class Upgrade extends Item {
        Upgrade(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player p,InteractionHand h){var stack=p.getItemInHand(h);if(!stack.is(PowerMod.ITEMS.get("drill_upgrade_block_placing")))return InteractionResult.PASS;if(!l.isClientSide()){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int slot=(tag.getIntOr("slot",0)+1)%9;tag.putInt("slot",slot);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));p.sendOverlayMessage(Component.literal("Placement hotbar slot: "+(slot+1)));}return InteractionResult.SUCCESS;}
    }
}
