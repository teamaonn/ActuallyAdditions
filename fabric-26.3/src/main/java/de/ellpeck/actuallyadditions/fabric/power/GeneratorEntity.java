package de.ellpeck.actuallyadditions.fabric.power;

import java.util.*;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.platform.energy.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

public final class GeneratorEntity extends BlockEntity implements WorldlyContainer, MenuProvider, BalmEnergyStorageProvider {
    public static final int CAPACITY = 60000, GENERATION = 20, OUTPUT = 80;
    private ItemStack fuel = ItemStack.EMPTY;
    private int stored, burn, maxBurn, sideCursor;
    private long outputTick = Long.MIN_VALUE;
    private int extracted;
    public GeneratorEntity(BlockPos pos, BlockState state) { super(PowerMod.GENERATOR_ENTITY, pos, state); }
    public int energy() { return stored; }
    public int burn() { return burn; }
    public int maxBurn() { return maxBurn; }
    public boolean paused() { return level != null && level.hasNeighborSignal(worldPosition); }
    private void refreshOutputBudget() {
        long now = level == null ? 0 : level.getGameTime();
        if (now != outputTick) { outputTick = now; extracted = 0; }
    }
    public final EnergyStorage storage = new EnergyStorage() {
        public int fill(int amount, boolean simulate) { return 0; }
        public int drain(int amount, boolean simulate) {
            refreshOutputBudget();
            int n = Math.min(Math.max(0, amount), Math.min(stored, OUTPUT - extracted));
            if (!simulate && n > 0) { stored -= n; extracted += n; setChanged(); }
            return n;
        }
        public int getEnergy() { return stored; }
        public void setEnergy(int value) { stored = Math.clamp(value, 0, CAPACITY); setChanged(); }
        public int getCapacity() { return CAPACITY; }
        public boolean canDrain() { return true; }
        public boolean canFill() { return false; }
    };
    @Override public EnergyStorage getEnergyStorage() { return storage; }
    public static int fuelTime(ServerLevel server, ItemStack stack) {
        var context = new LootContext.Builder(new LootParams(server, ContextMap.EMPTY, Map.of(), 0F)).create(Optional.empty());
        return Math.clamp(ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0), 0, 2400000);
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        boolean running = false;
        if (!paused() && stored <= CAPACITY - GENERATION) {
            if (burn == 0 && !fuel.isEmpty()) {
                int duration = fuelTime(server, fuel);
                if (duration > 0) {
                    var remainder = Balm.hooks().getCraftingRemainingItem(fuel);
                    fuel.shrink(1);
                    if (fuel.isEmpty() && remainder != null) fuel = remainder.create();
                    else if (remainder != null) Block.popResource(level, worldPosition, remainder.create());
                    burn = maxBurn = duration;
                }
            }
            if (burn > 0) { burn--; stored += GENERATION; running = true; setChanged(); }
        }
        // Output can drain the existing buffer while generation is paused by redstone.
        refreshOutputBudget();
        for (int i = 0; i < 6 && stored > 0 && extracted < OUTPUT; i++) {
            var side = Direction.values()[(i + sideCursor) % 6];
            var target = EnergyAccess.at(level, worldPosition.relative(side), side.getOpposite());
            if (target == null || !target.canFill()) continue;
            int offer = Math.min(stored, OUTPUT - extracted);
            int accepted = Math.clamp(target.fill(offer, false), 0, offer);
            if (accepted > 0) { stored -= accepted; extracted += accepted; setChanged(); }
        }
        sideCursor = (sideCursor + 1) % 6;
        if (getBlockState().getValue(PowerBlock.ACTIVE) != running) level.setBlock(worldPosition, getBlockState().setValue(PowerBlock.ACTIVE, running), 3);
    }
    public final ContainerData data = new ContainerData() {
        public int getCount() { return 4; }
        public int get(int i) { return switch (i) { case 0 -> stored; case 1 -> maxBurn == 0 ? 0 : (int)(1000L * burn / maxBurn); case 2 -> paused() ? 1 : 0; case 3 -> burn > 0 ? 1 : 0; default -> 0; }; }
        public void set(int i, int value) {}
    };
    @Override public Component getDisplayName() { return Component.translatable("block.actuallyadditions.coal_generator"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GeneratorMenu(id, inventory, this, data); }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return fuel.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? fuel : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int count) { var result = slot == 0 ? fuel.split(count) : ItemStack.EMPTY; setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { if (slot != 0) return ItemStack.EMPTY; var result = fuel; fuel = ItemStack.EMPTY; return result; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) { fuel = stack; setChanged(); } }
    @Override public void clearContent() { fuel = ItemStack.EMPTY; setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.has(DataComponents.COOKING_FUEL); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return side == Direction.DOWN && !stack.has(DataComponents.COOKING_FUEL); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel && !fuel.isEmpty()) Block.popResource(level, pos, fuel.copy());
        fuel = ItemStack.EMPTY; super.preRemoveSideEffects(pos, state);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!fuel.isEmpty()) out.store("fuel", ItemStack.CODEC, fuel);
        out.putInt("energy", stored); out.putInt("burn", burn); out.putInt("max_burn", maxBurn);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        fuel = in.read("fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        stored = Math.clamp(in.getIntOr("energy", 0), 0, CAPACITY);
        maxBurn = Math.clamp(in.getIntOr("max_burn", 0), 0, 2400000);
        burn = Math.clamp(in.getIntOr("burn", 0), 0, maxBurn);
    }
}
