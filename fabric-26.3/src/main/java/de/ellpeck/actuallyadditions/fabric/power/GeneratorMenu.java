package de.ellpeck.actuallyadditions.fabric.power;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class GeneratorMenu extends AbstractContainerMenu {
    private final Container container;
    public final ContainerData data;
    public GeneratorMenu(int id, Inventory inv) { this(id, inv, new SimpleContainer(1), new SimpleContainerData(4)); }
    public GeneratorMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(PowerMod.GENERATOR_MENU, id); this.container = container; this.data = data;
        addSlot(new Slot(container, 0, 79, 44) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(0, stack); } });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 170));
        addDataSlots(data);
    }
    public int energy() { return data.get(0) & 65535; }
    public int burnPercent() { return data.get(1); }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index == 0) { if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY; }
        else if (!slots.getFirst().mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
