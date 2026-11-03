package de.ellpeck.actuallyadditions.fabric.power;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class PowerClient implements ClientModInitializer {
    @Override public void onInitializeClient() { MenuScreens.register(PowerMod.GENERATOR_MENU, GeneratorScreen::new); }
}
