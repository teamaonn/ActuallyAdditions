package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
public final class Accessories {
    public static void initialize(){
        Content.item("ring_of_growth",p->new Ring(true,p),new Item.Properties());Content.item("ring_of_magnetizing",p->new Ring(false,p),new Item.Properties());
        Content.item("leaf_blower",p->new Blower(false,p),new Item.Properties());Content.item("advanced_leaf_blower",p->new Blower(true,p),new Item.Properties());Content.item("void_sack",VoidSack::new,new Item.Properties());
        Content.item("wings_of_the_bats",Wings::new,new Item.Properties().stacksTo(1));Content.item("player_probe",Probe::new,new Item.Properties().stacksTo(1));
        var goggles=Equippable.builder(EquipmentSlot.HEAD).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID,PowerMod.id("goggles"))).setDamageOnHurt(false).build();
        Content.item("engineers_goggles",Item::new,new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,goggles));Content.item("engineers_goggles_advanced",Item::new,new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,goggles));
        Content.item("laser_upgrade_invisibility",Item::new,new Item.Properties().stacksTo(1));Content.item("crate_keeper",Item::new,new Item.Properties().stacksTo(1));Content.item("item_tag",TagSelector::new,new Item.Properties().stacksTo(1));
        for(var name:List.of("drill","leaf_blo","phan_con","book")){String tag=name.equals("leaf_blo")?"leaf_blower":name;Content.item(name+"_pattern",Item::new,new Item.Properties().stacksTo(1).delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS,lookup->lookup.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(TagKey.create(Registries.BANNER_PATTERN,PowerMod.id("pattern_item/"+tag)))));}
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers())Wings.tick(p);});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->Wings.GRANTED.clear());
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->Wings.release(handler.player));
    }
    public static boolean canSeeLasers(Player p){var head=p.getItemBySlot(EquipmentSlot.HEAD);return head.is(PowerMod.ITEMS.get("engineers_goggles"))||head.is(PowerMod.ITEMS.get("engineers_goggles_advanced"))||p.getMainHandItem().is(PowerMod.ITEMS.get("laser_wrench"))||p.getOffhandItem().is(PowerMod.ITEMS.get("laser_wrench"));}
    public static final class Ring extends EnergyItem {
        private final boolean growth;Ring(boolean growth,Properties p){super(growth?1000000:200000,growth?2000:1000,p);this.growth=growth;}
        @Override public boolean isFoil(ItemStack s){return !enabled(s);}
        @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!p.isShiftKeyDown())return InteractionResult.PASS;if(!l.isClientSide()){toggle(p.getItemInHand(h));p.sendOverlayMessage(Component.literal(enabled(p.getItemInHand(h))?"Ring disabled":"Ring enabled"));}return InteractionResult.SUCCESS;}
        @Override public void inventoryTick(ItemStack s,ServerLevel l,Entity e,EquipmentSlot slot){
            if(!(e instanceof Player p)||p.isShiftKeyDown()||enabled(s))return;
            if(growth){if(p.getMainHandItem()!=s||l.getGameTime()%30!=0)return;List<BlockPos> plants=new ArrayList<>();for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-3,-3,-3),p.blockPosition().offset(3,3,3)))if(l.hasChunkAt(pos)&&l.getBlockState(pos).getBlock() instanceof BonemealableBlock&&!(l.getBlockState(pos).getBlock() instanceof GrassBlock))plants.add(pos.immutable());for(int i=0;i<45&&!plants.isEmpty()&&(p.isCreative()||energy(s)>=300);i++){var pos=plants.get(l.getRandom().nextInt(plants.size()));var before=l.getBlockState(pos);before.randomTick(l,pos,l.getRandom());if(l.getBlockState(pos)!=before)l.levelEvent(2005,pos,0);if(!p.isCreative())set(s,energy(s)-300);}}
            else if(!p.isCreative()&&!p.isSpectator())for(var item:l.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(5))){int before=item.getItem().getCount();if(!item.isAlive()||item.hasPickUpDelay()||energy(s)<before*50)continue;item.playerTouch(p);int picked=before-(item.isAlive()?item.getItem().getCount():0);if(picked>0)set(s,energy(s)-picked*50);}
        }
    }
    public static final class Blower extends Item {
        private final boolean advanced;Blower(boolean a,Properties p){super(p.stacksTo(1));advanced=a;}
        @Override public InteractionResult use(Level l,Player p,InteractionHand h){p.startUsingItem(h);return InteractionResult.SUCCESS;}
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return Integer.MAX_VALUE;}
        public boolean clear(ServerLevel server,net.minecraft.core.BlockPos center,int remaining,Player player){if(!advanced&&remaining%3!=0)return false;List<BlockPos> plants=new ArrayList<>();for(var pos:BlockPos.betweenClosed(center.offset(-5,advanced?-5:-1,-5),center.offset(5,advanced?5:1,5)))if(server.hasChunkAt(pos)&&(server.getBlockState(pos).getBlock() instanceof VegetationBlock||advanced&&server.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)))plants.add(pos.immutable());if(plants.isEmpty())return false;var pos=plants.get(server.getRandom().nextInt(plants.size()));if(player!=null&&!net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(server,player,pos,server.getBlockState(pos),null))return false;return server.destroyBlock(pos,true,player);}
        @Override public void onUseTick(Level l,LivingEntity e,ItemStack s,int remaining){if(l instanceof ServerLevel server)clear(server,e.blockPosition(),remaining,e instanceof Player p?p:null);}
    }
    public static final class VoidSack extends FilterItem {
        VoidSack(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player p,InteractionHand h){if(!p.isShiftKeyDown())return super.use(l,p,h);if(!l.isClientSide()){EnergyItem.toggle(p.getItemInHand(h));p.sendOverlayMessage(Component.literal(EnergyItem.enabled(p.getItemInHand(h))?"Void Sack collection enabled":"Void Sack collection disabled"));}return InteractionResult.SUCCESS;}
        @Override public void inventoryTick(ItemStack s,ServerLevel l,Entity e,EquipmentSlot slot){if(!(e instanceof Player p)||!EnergyItem.enabled(s))return;for(var item:l.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(1)))if(item.isAlive()&&!item.hasPickUpDelay()&&FilterItem.accepts(s,item.getItem()))item.discard();}
    }
    public static final class Wings extends Item {
        private static final Set<UUID> GRANTED=new HashSet<>();Wings(Properties p){super(p);}
        static int used(ItemStack s){return Math.clamp(s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("flight_used",0),0,800);}
        @Override public boolean isBarVisible(ItemStack s){return true;}@Override public int getBarWidth(ItemStack s){return 13-(int)(13L*used(s)/800);}@Override public int getBarColor(ItemStack s){return 0x4fafea;}
        static void release(ServerPlayer p){if(GRANTED.remove(p.getUUID())&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;}}
        static void tick(ServerPlayer p){
            ItemStack wings=ItemStack.EMPTY;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).getItem() instanceof Wings){wings=p.getInventory().getItem(i);break;}
            if(p.isCreative()||p.isSpectator()){GRANTED.remove(p.getUUID());return;}
            var data=wings.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int used=wings.isEmpty()?800:used(wings);
            boolean exhausted=data.getBooleanOr("flight_exhausted",false)||used>=800;if(used==0)exhausted=false;
            boolean may=!wings.isEmpty()&&!exhausted;
            if(may&&!p.getAbilities().mayfly){p.getAbilities().mayfly=true;GRANTED.add(p.getUUID());p.onUpdateAbilities();}
            if(!may&&GRANTED.remove(p.getUUID())){p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();}
            if(!wings.isEmpty()){int next=p.getAbilities().flying?Math.min(800,used+1):Math.max(0,used-2);data.putInt("flight_used",next);data.putBoolean("flight_exhausted",exhausted);wings.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
        }
    }
    public static final class TagSelector extends Item {
        TagSelector(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player p,InteractionHand h){var s=p.getItemInHand(h);var sample=p.getItemInHand(h==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);var tags=sample.getItem().builtInRegistryHolder().tags().sorted(Comparator.comparing(t->t.location().toString())).toList();if(tags.isEmpty())return InteractionResult.PASS;if(!l.isClientSide()){var data=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int i=Math.floorMod(data.getIntOr("next",0),tags.size());data.putString("item_tag",tags.get(i).location().toString());data.putInt("next",i+1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));p.sendOverlayMessage(Component.literal("Filter tag: #"+tags.get(i).location()));}return InteractionResult.SUCCESS;}
    }
    public static final class Probe extends Item {
        Probe(Properties p){super(p);}
        @Override public InteractionResult interactLivingEntity(ItemStack s,Player p,LivingEntity target,InteractionHand h){if(!(target instanceof Player player))return InteractionResult.PASS;if(!p.level().isClientSide()){var data=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putString("player",player.getUUID().toString());s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));p.sendOverlayMessage(Component.literal("Player selected: "+player.getName().getString()));}return InteractionResult.SUCCESS;}
        @Override public InteractionResult useOn(UseOnContext c){if(!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof WorkEntity e)||e.kind()!=WorkKind.PLAYER_INTERFACE||c.getPlayer()==null)return InteractionResult.PASS;if(!c.getLevel().isClientSide()){try{e.playerId=UUID.fromString(c.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("player",c.getPlayer().getUUID().toString()));e.setChanged();c.getPlayer().sendOverlayMessage(Component.literal("Player Interface bound; base range 32 blocks."));}catch(IllegalArgumentException ex){return InteractionResult.FAIL;}}return InteractionResult.SUCCESS;}
    }
}
