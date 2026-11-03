package de.ellpeck.actuallyadditions.fabric.power;

import java.util.*;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class PowerMod implements ModInitializer {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static PowerBlock GENERATOR;
    public static final List<PowerBlock> RELAYS = new ArrayList<>();
    public static BlockEntityType<GeneratorEntity> GENERATOR_ENTITY;
    public static BlockEntityType<RelayEntity> RELAY_ENTITY;
    public static MenuType<GeneratorMenu> GENERATOR_MENU;
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("actuallyadditions", path); }
    @Override public void onInitialize() {
        GENERATOR = block("coal_generator", -1);
        RELAYS.add(block("laser_relay", 0));
        RELAYS.add(block("laser_relay_advanced", 1));
        RELAYS.add(block("laser_relay_extreme", 2));
        item("laser_wrench", LaserWrench::new, new Item.Properties().stacksTo(1));
        item("laser_upgrade_range", Item::new, new Item.Properties());
        GENERATOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("coal_generator"),
                new BlockEntityType<>(GeneratorEntity::new, Set.of(GENERATOR)));
        RELAY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("energy_laser_relay"),
                new BlockEntityType<>(RelayEntity::new, new HashSet<>(RELAYS)));
        GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("coal_generator"), new MenuType<>(GeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));
        ItemStorage.SIDED.registerForBlockEntity((e, side) -> ContainerStorage.of(e, side), GENERATOR_ENTITY);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("power"), FabricCreativeModeTab.builder()
                .title(Component.literal("Actually Additions: Power"))
                .icon(() -> new ItemStack(GENERATOR)).displayItems((p, out) -> ITEMS.values().forEach(out::accept)).build());
    }
    private static PowerBlock block(String name, int tier) {
        var key = ResourceKey.create(Registries.BLOCK, id(name));
        var block = Registry.register(BuiltInRegistries.BLOCK, key, new PowerBlock(tier,
                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion().setId(key)));
        item(name, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }
    private static void item(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        var key = ResourceKey.create(Registries.ITEM, id(name));
        ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key))));
    }
}
