package de.ellpeck.actuallyadditions.fabric.power;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    public GeneratorScreen(GeneratorMenu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 196); inventoryLabelY = 100; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff30363d);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xffd4d4d4);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xff454c55);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xff9299a0);
        }
        g.fill(x + 12, y + 76, x + 164, y + 86, 0xff454c55);
        g.fill(x + 13, y + 77, x + 13 + (int)(150L * menu.energy() / GeneratorEntity.CAPACITY), y + 85, 0xffe2b43c);
        g.fill(x + 79, y + 30, x + 95, y + 38, 0xff454c55);
        g.fill(x + 80, y + 31, x + 80 + 14 * menu.burnPercent() / 1000, y + 37, 0xfff38a36);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, "Furnace fuel", 55, 20, 0xff303740, false);
        String status = menu.data.get(2) != 0 ? "Redstone pause" : menu.data.get(3) != 0 ? "20 energy/t" : "Add fuel";
        g.text(font, status, 12, 63, 0xff303740, false);
        g.text(font, menu.energy() + " / 60,000 energy", 12, 89, 0xff303740, false);
    }
}
