package de.ellpeck.actuallyadditions.fabric.power;

import java.util.*;
import net.blay09.mods.balm.platform.energy.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class RelayEntity extends BlockEntity implements BalmEnergyStorageProvider {
    public enum Mode { BOTH, INPUT_ONLY, OUTPUT_ONLY }
    public record Link(BlockPos pos, UUID node) {}
    private record Destination(BlockPos pos, EnergyStorage storage, List<RelayEntity> path) {}
    private static boolean routing;
    public UUID node = UUID.randomUUID();
    public final Map<BlockPos, UUID> links = new LinkedHashMap<>();
    public boolean rangeUpgrade,invisible;
    public Mode mode = Mode.BOTH;
    private long budgetTick = Long.MIN_VALUE, lastTransfer = Long.MIN_VALUE;
    private int used, cursor;
    public RelayEntity(BlockPos pos, BlockState state) { super(PowerMod.RELAY_ENTITY, pos, state); }
    public int tier() { return ((PowerBlock)getBlockState().getBlock()).tier; }
    public int capacity() { return switch (tier()) { case 1 -> 10000; case 2 -> 100000; default -> 1000; }; }
    public int lossPercent() { return switch (tier()) { case 1 -> 8; case 2 -> 10; default -> 5; }; }
    public int range() { return rangeUpgrade ? 35 : 15; }
    public String status() { return "Energy relay: " + mode + " | " + links.size() + " links | " + range() + " block range | " + capacity() + " energy/t"; }
    public void changed() { setChanged(); }
    public static String connect(RelayEntity a, RelayEntity b) {
        if (a == b || a.getBlockPos().equals(b.getBlockPos())) return "Choose a different relay.";
        if (a.level == null || a.level != b.level) return "Relays must be in the same dimension.";
        if (a.getBlockPos().distSqr(b.getBlockPos()) > (long)Math.min(a.range(), b.range()) * Math.min(a.range(), b.range())) return "Too far apart. Both relays need range upgrades beyond 15 blocks.";
        if (b.node.equals(a.links.get(b.getBlockPos())) && a.node.equals(b.links.get(a.getBlockPos()))) {
            a.links.remove(b.getBlockPos()); b.links.remove(a.getBlockPos()); a.changed(); b.changed(); return "Relay connection removed.";
        }
        a.links.put(b.getBlockPos().immutable(), b.node); b.links.put(a.getBlockPos().immutable(), a.node);
        a.changed(); b.changed(); return "Energy relays connected.";
    }
    public void unlinkAll() {
        if (level != null) for (var entry : links.entrySet()) {
            if (level.hasChunkAt(entry.getKey()) && level.getBlockEntity(entry.getKey()) instanceof RelayEntity other
                    && other.node.equals(entry.getValue()) && node.equals(other.links.get(worldPosition))) {
                other.links.remove(worldPosition); other.changed();
            }
        }
        links.clear(); changed();
    }
    private RelayEntity validPeer(BlockPos pos, UUID id) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        if (!(level.getBlockEntity(pos) instanceof RelayEntity other) || !other.node.equals(id) || !node.equals(other.links.get(worldPosition))) return null;
        return worldPosition.distSqr(pos) <= (long)Math.min(range(), other.range()) * Math.min(range(), other.range()) ? other : null;
    }
    private int available() {
        long tick = level == null ? 0 : level.getGameTime();
        if (tick != budgetTick) { budgetTick = tick; used = 0; }
        return Math.max(0, capacity() - used);
    }
    public final EnergyStorage storage = new EnergyStorage() {
        public int fill(int amount, boolean simulate) { return transfer(amount, simulate); }
        public int drain(int amount, boolean simulate) { return 0; }
        public int getEnergy() { return 0; }
        public void setEnergy(int amount) {}
        public int getCapacity() { return capacity(); }
        public boolean canFill() { return mode != Mode.OUTPUT_ONLY; }
        public boolean canDrain() { return false; }
    };
    @Override public EnergyStorage getEnergyStorage() { return storage; }
    /** Relays are pass-through nodes. Return source energy spent, including transmission loss. */
    public int transfer(int offered, boolean simulate) {
        if (routing || offered <= 0 || !(level instanceof ServerLevel) || mode == Mode.OUTPUT_ONLY) return 0;
        routing = true;
        try {
            Map<RelayEntity, List<RelayEntity>> paths = new LinkedHashMap<>();
            ArrayDeque<RelayEntity> queue = new ArrayDeque<>();
            paths.put(this, List.of(this)); queue.add(this);
            while (!queue.isEmpty()) {
                var relay = queue.removeFirst();
                for (var entry : relay.links.entrySet()) {
                    var peer = relay.validPeer(entry.getKey(), entry.getValue());
                    if (peer != null && !paths.containsKey(peer)) {
                        List<RelayEntity> path = new ArrayList<>(paths.get(relay)); path.add(peer);
                        paths.put(peer, path); queue.addLast(peer);
                    }
                }
            }
            // Each receiving block is offered energy once, even if multiple relays touch it.
            Map<BlockPos, Destination> destinations = new LinkedHashMap<>();
            for (var entry : paths.entrySet()) {
                var relay = entry.getKey();
                if (relay.mode == Mode.INPUT_ONLY) continue;
                for (var side : Direction.values()) {
                    var pos = relay.worldPosition.relative(side);
                    if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) instanceof RelayEntity) continue;
                    var target = EnergyAccess.at(level, pos, side.getOpposite());
                    if (target != null && target.canFill() && target.fill(1, true) > 0) destinations.putIfAbsent(pos, new Destination(pos, target, entry.getValue()));
                }
            }
            if (destinations.isEmpty()) return 0;
            List<Destination> ordered = new ArrayList<>(destinations.values());
            Collections.rotate(ordered, -(cursor % ordered.size()));
            int remaining = Math.min(offered, available()), spent = 0;
            Map<RelayEntity, Integer> simulatedUsage = new IdentityHashMap<>();
            for (int i = 0; i < ordered.size() && remaining > 0; i++) {
                var dest = ordered.get(i);
                int allowance = remaining;
                int loss = 0;
                for (var relay : dest.path) {
                    allowance = Math.min(allowance, relay.available() - simulatedUsage.getOrDefault(relay, 0));
                    loss = Math.max(loss, relay.lossPercent());
                }
                allowance = Math.min(allowance, Math.max(1, remaining / (ordered.size() - i)));
                if (allowance <= 0) continue;
                // Match the original small-packet exemption rather than blocking 1-unit packets.
                int receivedOffer = allowance <= 1 ? allowance : allowance * (100 - loss) / 100;
                if (receivedOffer <= 0) continue;
                int accepted = Math.clamp(dest.storage.fill(receivedOffer, simulate), 0, receivedOffer);
                if (accepted <= 0) continue;
                int cost = allowance <= 1 ? accepted : (accepted * 100 + (100 - loss) - 1) / (100 - loss);
                cost = Math.min(cost, allowance);
                remaining -= cost; spent += cost;
                for (var relay : dest.path) {
                    if (simulate) simulatedUsage.merge(relay, cost, Integer::sum);
                    else { relay.used += cost; relay.lastTransfer = level.getGameTime(); }
                }
            }
            if (!simulate && spent > 0) cursor++;
            return spent;
        } finally { routing = false; }
    }
    public void tick() {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 10 != Math.floorMod(worldPosition.asLong(), 10)
                || lastTransfer < server.getGameTime() - 15) return;
        for (var entry : links.entrySet()) {
            if (worldPosition.compareTo(entry.getKey()) >= 0 || validPeer(entry.getKey(), entry.getValue()) == null) continue;
            var start = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition); var delta = net.minecraft.world.phys.Vec3.atCenterOf(entry.getKey()).subtract(start);
            int points = Math.max(2, (int)(delta.length() * 2));
            for (int i = 0; i <= points; i++) {
                var p = start.add(delta.scale(i / (double)points));
                if(invisible||validPeer(entry.getKey(),entry.getValue()).invisible){for(var player:server.players())if(de.ellpeck.actuallyadditions.fabric.content.Accessories.canSeeLasers(player))server.sendParticles(player,new DustParticleOptions(0x45A8FF,.6F),false,false,p.x,p.y,p.z,1,0,0,0,0);}else server.sendParticles(new DustParticleOptions(0x45A8FF, 0.6F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel) {
            unlinkAll();
            if(invisible)Block.popResource(level,pos,new net.minecraft.world.item.ItemStack(PowerMod.ITEMS.get("laser_upgrade_invisibility")));
            if (rangeUpgrade) Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(PowerMod.ITEMS.get("laser_upgrade_range")));
        }
        super.preRemoveSideEffects(pos, state);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("node", node.toString()); out.putString("mode", mode.name()); out.putBoolean("range_upgrade", rangeUpgrade);out.putBoolean("invisible",invisible);
        out.putInt("links", links.size()); int i = 0;
        for (var entry : links.entrySet()) {
            var child = out.child("link_" + i++); child.putInt("x", entry.getKey().getX()); child.putInt("y", entry.getKey().getY()); child.putInt("z", entry.getKey().getZ()); child.putString("node", entry.getValue().toString());
        }
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        try { node = UUID.fromString(in.getStringOr("node", node.toString())); } catch (IllegalArgumentException ignored) {}
        try { mode = Mode.valueOf(in.getStringOr("mode", "BOTH")); } catch (IllegalArgumentException ignored) { mode = Mode.BOTH; }
        rangeUpgrade = in.getBooleanOr("range_upgrade", false);invisible=in.getBooleanOr("invisible",false);
        links.clear();
        int size = Math.clamp(in.getIntOr("links", 0), 0, 4096);
        for (int i = 0; i < size; i++) in.child("link_" + i).ifPresent(child -> {
            try { links.put(new BlockPos(child.getIntOr("x", 0), child.getIntOr("y", 0), child.getIntOr("z", 0)), UUID.fromString(child.getStringOr("node", ""))); } catch (IllegalArgumentException ignored) {}
        });
    }
}
