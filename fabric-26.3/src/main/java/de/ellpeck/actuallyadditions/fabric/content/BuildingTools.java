package de.ellpeck.actuallyadditions.fabric.content;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
public final class BuildingTools {
    public static void initialize(){Content.item("handheld_filler",Filler::new,new Item.Properties());Content.item("water_bowl",WaterBowl::new,new Item.Properties());net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register(WaterBowl::fill);}
    public static final class Filler extends EnergyItem {
        Filler(Properties p){super(500000,1000,p);}
        @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()==null)return InteractionResult.PASS;if(c.getLevel().isClientSide())return InteractionResult.SUCCESS;var stack=c.getItemInHand();var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(c.getPlayer().isShiftKeyDown()){data.store("selected",BlockState.CODEC,c.getLevel().getBlockState(c.getClickedPos()));data.remove("first");data.remove("last");c.getPlayer().sendOverlayMessage(Component.literal("Filler material selected"));}
            else if(data.read("selected",BlockState.CODEC).isPresent()){if(!data.contains("first")||data.contains("last")){data.putLong("first",c.getClickedPos().asLong());data.remove("last");c.getPlayer().sendOverlayMessage(Component.literal("First corner selected"));}else{data.putLong("last",c.getClickedPos().asLong());data.putLong("index",0);c.getPlayer().sendOverlayMessage(Component.literal("Filling selected area while held"));}}
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return InteractionResult.SUCCESS;
        }
        @Override public void inventoryTick(ItemStack s,ServerLevel l,Entity e,EquipmentSlot slot){if(!(e instanceof Player p)||p.getMainHandItem()!=s)return;var data=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(!data.contains("first")||!data.contains("last"))return;var state=data.read("selected",BlockState.CODEC).orElse(null);if(state==null||state.isAir()||!p.isCreative()&&energy(s)<1500)return;
            var a=BlockPos.of(data.getLongOr("first",0));var b=BlockPos.of(data.getLongOr("last",0));long dx=Math.abs((long)a.getX()-b.getX())+1,dy=Math.abs((long)a.getY()-b.getY())+1,dz=Math.abs((long)a.getZ()-b.getZ())+1,index=Math.max(0,data.getLongOr("index",0));long volume;try{volume=Math.multiplyExact(Math.multiplyExact(dx,dy),dz);}catch(ArithmeticException ex){data.remove("first");data.remove("last");s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return;}if(index>=volume){data.remove("first");data.remove("last");s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return;}
            var pos=new BlockPos((int)(Math.min(a.getX(),b.getX())+index%dx),(int)(Math.min(a.getY(),b.getY())+index/dx%dy),(int)(Math.min(a.getZ(),b.getZ())+index/(dx*dy)));if(!l.hasChunkAt(pos))return;
            if(l.getBlockState(pos).canBeReplaced()&&state.canSurvive(l,pos)&&l.mayInteract(p,pos)&&p.mayUseItemAt(pos,Direction.UP,s)){ItemStack material=ItemStack.EMPTY;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(state.getBlock().asItem())){material=p.getInventory().getItem(i);break;}if(!p.isCreative()&&material.isEmpty())return;if(l.setBlockAndUpdate(pos,state)&&!p.isCreative()){material.shrink(1);set(s,energy(s)-1500);data=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}}
            data.putLong("index",index+1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        }
    }
    public static final class WaterBowl extends BucketItem {
        WaterBowl(Properties p){super(Fluids.WATER,p.stacksTo(1));}
        @Override public InteractionResult use(Level l,Player p,InteractionHand h){var hit=getPlayerPOVHitResult(l,p,ClipContext.Fluid.NONE);if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResult.PASS;var pos=hit.getBlockPos().relative(hit.getDirection());var s=p.getItemInHand(h);if(!l.mayInteract(p,pos)||!p.mayUseItemAt(pos,hit.getDirection(),s))return InteractionResult.FAIL;if(l.isClientSide())return InteractionResult.SUCCESS;if(emptyContents(p,l,pos,hit)){p.setItemInHand(h,ItemUtils.createFilledResult(s,p,new ItemStack(Items.BOWL)));return InteractionResult.SUCCESS;}return InteractionResult.FAIL;}
        @Override public void inventoryTick(ItemStack s,ServerLevel l,Entity e,EquipmentSlot slot){if(!(e instanceof Player p)||!PortConfig.bool("other","waterBowlSpilling")||l.getGameTime()%10!=0||l.getRandom().nextBoolean())return;var data=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int x=p.blockPosition().getX(),y=p.blockPosition().getY();boolean moved=data.contains("bowl_x")&&(data.getIntOr("bowl_x",x)!=x||data.getIntOr("bowl_y",y)!=y);if(moved&&!p.isShiftKeyDown()&&emptyContents(p,l,p.blockPosition(),null)){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==s){p.getInventory().setItem(i,new ItemStack(Items.BOWL));return;}}data.putInt("bowl_x",x);data.putInt("bowl_y",y);s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
        static InteractionResult fill(Player p,Level l,InteractionHand h){var s=p.getItemInHand(h);if(!PortConfig.bool("other","waterBowl")||!s.is(Items.BOWL))return InteractionResult.PASS;var hit=getPlayerPOVHitResult(l,p,ClipContext.Fluid.SOURCE_ONLY);if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResult.PASS;var pos=hit.getBlockPos();var state=l.getBlockState(pos);if(!state.getFluidState().isSourceOfType(Fluids.WATER)||!l.mayInteract(p,pos)||!(state.getBlock() instanceof BucketPickup pickup))return InteractionResult.PASS;if(!l.isClientSide()&&!pickup.pickupBlock(p,l,pos,state).isEmpty())p.setItemInHand(h,ItemUtils.createFilledResult(s,p,new ItemStack(PowerMod.ITEMS.get("water_bowl"))));return InteractionResult.SUCCESS;}
    }
}
