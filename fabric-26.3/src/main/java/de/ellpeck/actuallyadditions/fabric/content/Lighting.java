package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerBlock;
public final class Lighting {
    private static final Map<Level,Set<BlockPos>> SUPPRESSORS=new WeakHashMap<>();
    public static List<BlockPos> protect(ServerLevel l,List<BlockPos> blocks){var nodes=SUPPRESSORS.get(l);if(nodes==null)return blocks;List<WorkEntity> active=new ArrayList<>();nodes.removeIf(p->{if(!l.hasChunkAt(p))return true;var e=l.getBlockEntity(p);if(e instanceof WorkEntity w&&w.kind()==WorkKind.SHOCK&&!w.isRemoved()){active.add(w);return false;}return true;});List<BlockPos> result=new ArrayList<>();for(var p:blocks){boolean protectedBlock=false;for(var e:active)if(e.getBlockPos().distSqr(p)<=25&&e.spend(300)){protectedBlock=true;break;}if(!protectedBlock)result.add(p);}return result;}
    public static final class State {
        private final WorkEntity e;private final ArrayDeque<BlockPos> queue=new ArrayDeque<>();private final Set<BlockPos> seen=new HashSet<>();private boolean last;private int ticks;
        public final int[] settings={2,2,2,65,25,3,100,0,0,0,0,2};
        State(WorkEntity e){this.e=e;}
        public void adjust(int i,int delta){if(i<0||i>=12)return;int max=i==3||i==4||i>=6&&i<=10?100:i==11?64:i==2?3:6,min=i==1||i==2||i==5?1:0;settings[i]=Math.clamp(settings[i]+delta*(max==100?5:1),min,max);e.setChanged();}
        public void save(ValueOutput out){out.putIntArray("settings",settings);}
        public void load(ValueInput in){in.getIntArray("settings").ifPresent(values->{for(int i=0;i<Math.min(12,values.length);i++){settings[i]=values[i];adjust(i,0);}});}
        public boolean tick(boolean powered){var l=e.getLevel();
            if(e.kind()==WorkKind.SHOCK){SUPPRESSORS.computeIfAbsent(l,k->new HashSet<>()).add(e.getBlockPos());return false;}
            if(e.kind()==WorkKind.FIREWORK){boolean fire=e.pulseMode?powered&&!last:!powered&&++ticks>=100;last=powered;if(fire){ticks=0;return launch();}return false;}
            if(powered!=last||++ticks>=20&&queue.isEmpty()){ticks=0;queue.clear();seen.clear();queue.add(e.getBlockPos().relative(e.getBlockState().getValue(PowerBlock.FACING)));}last=powered;
            for(int i=0;i<128&&!queue.isEmpty();i++){var p=queue.removeFirst();if(!seen.add(p)||!l.hasChunkAt(p))continue;var state=l.getBlockState(p);if(!(state.getBlock() instanceof Decorations.Lamp))continue;if(state.getValue(PowerBlock.ACTIVE)!=powered)l.setBlockAndUpdate(p,state.setValue(PowerBlock.ACTIVE,powered));for(var d:Direction.values())if(!seen.contains(p.relative(d)))queue.add(p.relative(d));}if(queue.isEmpty())seen.clear();return false;
        }
        int random(int value){return Math.clamp(value+e.getLevel().getRandom().nextInt(settings[0]*2+1)-settings[0],1,6);}
        public boolean launch(){if(e.energy()<500)return false;var l=e.getLevel();var r=l.getRandom();List<FireworkExplosion> explosions=new ArrayList<>();for(int n=0;n<random(settings[1]);n++){var colors=new it.unimi.dsi.fastutil.ints.IntArrayList();for(int i=0;i<random(settings[5]);i++)colors.add(DyeColor.values()[r.nextInt(16)].getFireworkColor());int total=0;for(int i=6;i<11;i++)total+=settings[i];int type=0;if(total>0){int roll=r.nextInt(total);for(int i=6;i<11;i++){roll-=settings[i];if(roll<0){type=i-6;break;}}}boolean effect=r.nextInt(100)<settings[3],flicker=effect&&r.nextInt(100)<settings[4];explosions.add(new FireworkExplosion(FireworkExplosion.Shape.byId(type),colors,it.unimi.dsi.fastutil.ints.IntList.of(),effect&&!flicker,flicker));}var item=new ItemStack(Items.FIREWORK_ROCKET);item.set(DataComponents.FIREWORKS,new Fireworks(settings[2],explosions));var p=e.getBlockPos();double x=p.getX()+.5+(r.nextDouble()*2-1)*settings[11],z=p.getZ()+.5+(r.nextDouble()*2-1)*settings[11];if(!l.hasChunkAt(BlockPos.containing(x,p.getY()+1,z)))return false;if(l.addFreshEntity(new net.minecraft.world.entity.projectile.FireworkRocketEntity(l,x,p.getY()+1,z,item))){e.spend(500);return true;}return false;}
    }
}
