package de.ellpeck.actuallyadditions.fabric.power;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class PowerClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        de.ellpeck.actuallyadditions.fabric.content.ClientRecipes.initialize();
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(de.ellpeck.actuallyadditions.fabric.content.Content.ENTITY,de.ellpeck.actuallyadditions.fabric.content.DisplayRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(de.ellpeck.actuallyadditions.fabric.content.WormEntity.TYPE,net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        MenuScreens.register(de.ellpeck.actuallyadditions.fabric.content.Content.GUIDE_MENU,de.ellpeck.actuallyadditions.fabric.content.GuideScreen::new);
        for(var f:de.ellpeck.actuallyadditions.fabric.content.Oils.ALL.values()){
            var still=new net.minecraft.client.resources.model.sprite.Material(PowerMod.id("block/fluid/"+f.name+"_still"));
            var flowing=new net.minecraft.client.resources.model.sprite.Material(PowerMod.id("block/fluid/"+f.name+"_flowing"));
            net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(f.source,f.flowing,new net.minecraft.client.renderer.block.FluidModel.Unbaked(still,flowing,null,state->0xffffffff));
        }
        MenuScreens.register(de.ellpeck.actuallyadditions.fabric.content.Content.ITEM_MENU,de.ellpeck.actuallyadditions.fabric.content.ItemSlotsScreen::new);
        MenuScreens.register(de.ellpeck.actuallyadditions.fabric.content.Content.FILTER_MENU,de.ellpeck.actuallyadditions.fabric.content.FilterScreen::new);
        MenuScreens.register(PowerMod.GENERATOR_MENU, GeneratorScreen::new); MenuScreens.register(de.ellpeck.actuallyadditions.fabric.content.Content.MENU, de.ellpeck.actuallyadditions.fabric.content.WorkScreen::new); }
}
