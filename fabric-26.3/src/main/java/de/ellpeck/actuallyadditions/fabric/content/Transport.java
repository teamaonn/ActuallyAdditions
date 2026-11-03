package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import de.ellpeck.actuallyadditions.fabric.power.PowerBlock;
/** Loaded-node networks. UUIDs reject replaced nodes; transfer budgets participate in transactions. */
public final class Transport {
    private static boolean routing;
    public static final class Node extends SnapshotParticipant<Long> {
        final WorkEntity owner;public UUID id=UUID.randomUUID();public final Map<BlockPos,UUID> links=new LinkedHashMap<>();
        public int mode,priority;public boolean rangeUpgrade,invisible;private long tick=Long.MIN_VALUE,used,lastTransfer=Long.MIN_VALUE;
        Node(WorkEntity owner){this.owner=owner;}
        public int range(){return rangeUpgrade?35:15;}
        boolean fluid(){return owner.kind()==WorkKind.FLUID_RELAY;}
        long available(){long time=owner.getLevel().getGameTime();if(tick!=time){tick=time;used=0;}return Math.max(0,81L*PortConfig.number("fluidLaserTransferRate",1,Integer.MAX_VALUE)-used);}
        void spend(long n,TransactionContext tx){available();updateSnapshots(tx);used+=n;}
        @Override protected void onFinalCommit(){lastTransfer=owner.getLevel().getGameTime();}
        @Override protected Long createSnapshot(){return used;}
        @Override protected void readSnapshot(Long value){used=value;}
        Node peer(BlockPos p,UUID expected){var l=owner.getLevel();if(l==null||!l.hasChunkAt(p)||!(l.getBlockEntity(p) instanceof WorkEntity e)||!e.isTransportRelay())return null;var n=e.network;int r=Math.min(range(),n.range());return fluid()==n.fluid()&&expected.equals(n.id)&&id.equals(n.links.get(owner.getBlockPos()))&&owner.getBlockPos().distSqr(p)<=(long)r*r?n:null;}
        public void unlink(){for(var e:links.entrySet()){var n=peer(e.getKey(),e.getValue());if(n!=null){n.links.remove(owner.getBlockPos());n.owner.setChanged();}}links.clear();owner.setChanged();}
        public void save(ValueOutput out){out.putString("id",id.toString());out.putInt("mode",mode);out.putInt("priority",priority);out.putBoolean("range",rangeUpgrade);out.putBoolean("invisible",invisible);out.putInt("size",links.size());int i=0;for(var e:links.entrySet()){var c=out.child("link_"+i++);c.store("pos",BlockPos.CODEC,e.getKey());c.putString("id",e.getValue().toString());}}
        public void load(ValueInput in){try{id=UUID.fromString(in.getStringOr("id",id.toString()));}catch(IllegalArgumentException ignored){}mode=Math.clamp(in.getIntOr("mode",0),0,2);priority=Math.clamp(in.getIntOr("priority",0),-10000,10000);rangeUpgrade=in.getBooleanOr("range",false);invisible=in.getBooleanOr("invisible",false);links.clear();for(int i=0;i<Math.clamp(in.getIntOr("size",0),0,4096);i++)in.child("link_"+i).ifPresent(c->{try{c.read("pos",BlockPos.CODEC).ifPresent(p->links.put(p,UUID.fromString(c.getStringOr("id",""))));}catch(IllegalArgumentException ignored){}});}
    }
    public static String connect(Node a,Node b){
        if(a==b)return "Choose another relay.";if(a.owner.getLevel()!=b.owner.getLevel()||a.fluid()!=b.fluid())return "Connect relays of the same resource type.";int r=Math.min(a.range(),b.range());if(a.owner.getBlockPos().distSqr(b.owner.getBlockPos())>(long)r*r)return "Out of range; upgrade both ends.";
        if(b.id.equals(a.links.get(b.owner.getBlockPos()))&&a.id.equals(b.links.get(a.owner.getBlockPos()))){a.links.remove(b.owner.getBlockPos());b.links.remove(a.owner.getBlockPos());}else{a.links.put(b.owner.getBlockPos(),b.id);b.links.put(a.owner.getBlockPos(),a.id);}a.owner.setChanged();b.owner.setChanged();return "Relay connection updated.";
    }
    private static Map<Node,List<Node>> paths(Node root){var paths=new LinkedHashMap<Node,List<Node>>();var queue=new ArrayDeque<Node>();paths.put(root,List.of(root));queue.add(root);while(!queue.isEmpty()){var n=queue.removeFirst();for(var e:n.links.entrySet()){var peer=n.peer(e.getKey(),e.getValue());if(peer==null||paths.containsKey(peer))continue;var path=new ArrayList<>(paths.get(n));path.add(peer);paths.put(peer,List.copyOf(path));queue.add(peer);}}return paths;}
    private static boolean excluded(BlockEntity e){return e instanceof WorkEntity w&&(w.isTransportRelay()||w.kind()==WorkKind.ITEM_INTERFACE||w.kind()==WorkKind.HOPPING_INTERFACE);}
    private record Port<T>(Node node,BlockPos pos,BlockEntity entity,Storage<T> storage,List<Node> path){
        boolean loaded(){var l=node.owner.getLevel();return l.hasChunkAt(pos)&&l.getBlockEntity(pos)==entity&&!entity.isRemoved();}
        boolean allows(T resource,boolean insert){if(!loaded()||insert&&node.mode==1||!insert&&node.mode==2)return false;return !(resource instanceof ItemVariant item)||node.owner.kind()!=WorkKind.ADVANCED_ITEM_RELAY||FilterItem.accepts(node.owner.getItem(insert?0:1),item.toStack());}
    }
    private static Node root(WorkEntity owner){var l=owner.getLevel();for(var side:Direction.values()){var p=owner.getBlockPos().relative(side);if(l.hasChunkAt(p)&&l.getBlockEntity(p) instanceof WorkEntity e&&e.isTransportRelay()&&!e.network.fluid())return e.network;}return null;}
    private static List<Port<ItemVariant>> itemPorts(Node root){var result=new ArrayList<Port<ItemVariant>>();Set<String> seen=new HashSet<>();var entries=paths(root).entrySet().stream().sorted(Comparator.<Map.Entry<Node,List<Node>>>comparingInt(e->e.getKey().priority).reversed()).toList();for(var e:entries){var n=e.getKey();var l=n.owner.getLevel();for(var side:Direction.values()){var p=n.owner.getBlockPos().relative(side);if(!l.hasChunkAt(p)||!seen.add(p+"/"+side))continue;var entity=l.getBlockEntity(p);if(entity==null||excluded(entity))continue;var storage=ItemStorage.SIDED.find(l,p,side.getOpposite());if(storage!=null)result.add(new Port<>(n,p,entity,storage,e.getValue()));}}return result;}
    private static boolean filterRoot(Node n,ItemVariant resource,boolean insert){return n.owner.kind()!=WorkKind.ADVANCED_ITEM_RELAY||FilterItem.accepts(n.owner.getItem(insert?0:1),resource.toStack());}
    public static Storage<ItemVariant> items(WorkEntity owner){return new Storage<>(){
        public long insert(ItemVariant resource,long amount,TransactionContext tx){return transfer(resource,amount,tx,true);}
        public long extract(ItemVariant resource,long amount,TransactionContext tx){return transfer(resource,amount,tx,false);}
        private long transfer(ItemVariant resource,long amount,TransactionContext tx,boolean insert){if(routing||amount<=0)return 0;var r=root(owner);if(r==null||!filterRoot(r,resource,insert))return 0;routing=true;try{long used=0;for(var p:itemPorts(r)){if(!p.allows(resource,insert))continue;long n=insert?p.storage.insert(resource,amount-used,tx):p.storage.extract(resource,amount-used,tx);used+=n;if(n>0)for(var node:p.path)node.spend(0,tx);if(used==amount)break;}return used;}finally{routing=false;}}
        public Iterator<StorageView<ItemVariant>> iterator(){if(routing)return Collections.emptyIterator();var r=root(owner);if(r==null)return Collections.emptyIterator();routing=true;try{List<StorageView<ItemVariant>> views=new ArrayList<>();for(var p:itemPorts(r))for(var view:p.storage)views.add(new StorageView<>(){
            public boolean isResourceBlank(){return !p.loaded()||view.isResourceBlank();}public ItemVariant getResource(){return view.getResource();}public long getAmount(){return p.loaded()?view.getAmount():0;}public long getCapacity(){return p.loaded()?view.getCapacity():0;}
            public long extract(ItemVariant resource,long amount,TransactionContext tx){if(routing||root(owner)!=r||!paths(r).containsKey(p.node)||!filterRoot(r,resource,false)||!p.allows(resource,false))return 0;routing=true;try{long n=view.extract(resource,amount,tx);if(n>0)for(var node:p.path)node.spend(0,tx);return n;}finally{routing=false;}}
        });return views.iterator();}finally{routing=false;}}
    };}
    public static Storage<FluidVariant> fluids(WorkEntity owner,Direction from){return new Storage<>(){
        public long insert(FluidVariant fluid,long amount,TransactionContext tx){if(routing||amount<=0||owner.network.mode==2)return 0;routing=true;try{var source=from==null?null:owner.getBlockPos().relative(from);long used=0;Set<String> seen=new HashSet<>();for(var e:paths(owner.network).entrySet()){var n=e.getKey();if(n==owner.network||n.mode==1)continue;var l=n.owner.getLevel();for(var side:Direction.values()){var p=n.owner.getBlockPos().relative(side);if(p.equals(source)||!l.hasChunkAt(p)||!seen.add(p+"/"+side))continue;var entity=l.getBlockEntity(p);if(entity==null||excluded(entity))continue;var target=FluidStorage.SIDED.find(l,p,side.getOpposite());if(target==null)continue;long limit=amount-used;for(var path:e.getValue())limit=Math.min(limit,path.available());if(limit<=0)return used;long transferred=target.insert(fluid,limit,tx);if(transferred>0){used+=transferred;for(var path:e.getValue())path.spend(transferred,tx);}if(used==amount)return used;}}return used;}finally{routing=false;}}
        public long extract(FluidVariant f,long n,TransactionContext t){return 0;}public boolean supportsExtraction(){return false;}public Iterator<StorageView<FluidVariant>> iterator(){return Collections.emptyIterator();}
    };}
    public static void tick(WorkEntity owner){
        var l=owner.getLevel();
        if(owner.isTransportRelay()&&l instanceof net.minecraft.server.level.ServerLevel server&&l.getGameTime()%10==Math.floorMod(owner.getBlockPos().asLong(),10))for(var link:owner.network.links.entrySet()){
            var peer=owner.network.peer(link.getKey(),link.getValue());if(peer==null||owner.getBlockPos().compareTo(link.getKey())>=0)continue;var start=net.minecraft.world.phys.Vec3.atCenterOf(owner.getBlockPos());var delta=net.minecraft.world.phys.Vec3.atCenterOf(link.getKey()).subtract(start);int count=Math.max(2,(int)(delta.length()*2));
            for(int i=0;i<=count;i++){var p=start.add(delta.scale(i/(double)count));var particle=new net.minecraft.core.particles.DustParticleOptions(owner.network.fluid()?0x3288ff:0x24b34b,.5F);if(!owner.network.invisible&&!peer.invisible&&(owner.network.lastTransfer>=l.getGameTime()-15||peer.lastTransfer>=l.getGameTime()-15))server.sendParticles(particle,p.x,p.y,p.z,1,0,0,0,0);else for(var player:server.players())if(Accessories.canSeeLasers(player))server.sendParticles(player,particle,false,false,p.x,p.y,p.z,1,0,0,0,0);}
        }
        if(owner.kind()==WorkKind.FLUID_RELAY&&owner.network.mode==1){for(var side:Direction.values()){var p=owner.getBlockPos().relative(side);if(!l.hasChunkAt(p)||excluded(l.getBlockEntity(p)))continue;var storage=FluidStorage.SIDED.find(l,p,side.getOpposite());if(storage!=null)try(var tx=Transaction.openOuter()){StorageUtil.move(storage,fluids(owner,side),r->true,owner.network.available(),tx);tx.commit();}}}
        if(owner.kind()!=WorkKind.HOPPING_INTERFACE||l.getGameTime()%10!=0||l.hasNeighborSignal(owner.getBlockPos()))return;var network=items(owner);var above=owner.getBlockPos().above();var side=owner.getBlockState().getValue(PowerBlock.FACING);var below=owner.getBlockPos().relative(side);
        if(l.hasChunkAt(above)&&!excluded(l.getBlockEntity(above))){var source=ItemStorage.SIDED.find(l,above,Direction.DOWN);if(source!=null){try(var tx=Transaction.openOuter()){StorageUtil.move(source,network,r->true,4,tx);tx.commit();}}else for(var item:l.getEntitiesOfClass(ItemEntity.class,new AABB(above).inflate(0,.5,0)))if(item.isAlive()&&!item.hasPickUpDelay())try(var tx=Transaction.openOuter()){long n=network.insert(ItemVariant.of(item.getItem()),item.getItem().getCount(),tx);tx.commit();item.getItem().shrink((int)n);if(item.getItem().isEmpty())item.discard();}}
        if(l.hasChunkAt(below)&&!excluded(l.getBlockEntity(below))){var target=ItemStorage.SIDED.find(l,below,side.getOpposite());if(target!=null)try(var tx=Transaction.openOuter()){StorageUtil.move(network,target,r->true,4,tx);tx.commit();}}
    }
}
