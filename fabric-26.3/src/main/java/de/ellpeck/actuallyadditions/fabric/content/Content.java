package de.ellpeck.actuallyadditions.fabric.content;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class Content {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final Map<WorkKind,WorkBlock> MACHINES=new EnumMap<>(WorkKind.class);
    public static BlockEntityType<WorkEntity> ENTITY;
    public static MenuType<WorkMenu> MENU;
    public static MenuType<FilterMenu> FILTER_MENU;
    public static MenuType<ItemSlotsMenu> ITEM_MENU;
    public static MenuType<GuideMenu> GUIDE_MENU;
    public static void initialize(){
        try(var stream=Objects.requireNonNull(Content.class.getResourceAsStream("/aa_content.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var data=JsonParser.parseReader(reader).getAsJsonObject();
            for(var row:data.getAsJsonArray("items")){
                var name=row.getAsString();var properties=new Item.Properties();
                if(name.equals("tiny_coal")||name.equals("tiny_charcoal"))properties.component(net.minecraft.core.component.DataComponents.COOKING_FUEL,new net.minecraft.world.item.component.CookingFuel(new net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant(200),new net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat.Constant(1F)));
                if(name.startsWith("empowered_")||name.equals("ender_star"))properties.component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
                item(name,Item::new,properties);
            }
            for(var row:data.getAsJsonArray("blocks")){
                var obj=row.getAsJsonObject();String name=obj.get("id").getAsString(),type=obj.get("kind").getAsString();
                block(name,p->switch(type){
                    case "slab"->new SlabBlock(p);case "stairs"->new StairBlock(Blocks.STONE.defaultBlockState(),p);
                    case "wall"->new WallBlock(p);default->new Block(p);
                },BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2F));
            }
        }catch(IOException e){throw new UncheckedIOException(e);}
        int[] capacities={200000,350000,600000,1000000,2000000}, rates={1000,5000,10000,30000,100000};
        String[] batteries={"single_battery","double_battery","triple_battery","quadruple_battery","quintuple_battery"};
        for(int i=0;i<batteries.length;i++){final int tier=i;item(batteries[i],p->new EnergyItem(capacities[tier],rates[tier],p),new Item.Properties());}
        PreservingRecipe.register();
        PreservingShapelessRecipe.register();
        Oils.initialize();
        Crops.initialize();
        UtilityItems.initialize();
        DrillItem.initialize();
        Lenses.initialize();
        Coffee.initialize();
        Accessories.initialize();
        BuildingTools.initialize();
        WormEntity.initialize();
        item("booklet",p->new Item(p.stacksTo(1)){@Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand h){if(!l.isClientSide())player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,viewer)->new GuideMenu(id,inv),net.minecraft.network.chat.Component.literal("Actually Additions Manual")));return net.minecraft.world.InteractionResult.SUCCESS;}},new Item.Properties());
        Decorations.initialize();
        block("phantom_booster",Block::new,BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F));
        item("phantom_connector",PhantomConnector::new,new Item.Properties());
        item("filter",FilterItem::new,new Item.Properties());
        item("solidified_experience",SolidExperience::new,new Item.Properties());
        for(var kind:WorkKind.values())MACHINES.put(kind,(WorkBlock)block(kind.id,p->new WorkBlock(kind,p),BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion()));
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,PowerMod.id("work_machine"),new BlockEntityType<>(WorkEntity::new,new HashSet<>(MACHINES.values())));
        GUIDE_MENU=Registry.register(BuiltInRegistries.MENU,PowerMod.id("guide"),new MenuType<>(GuideMenu::new,FeatureFlags.DEFAULT_FLAGS));
        ITEM_MENU=Registry.register(BuiltInRegistries.MENU,PowerMod.id("item_inventory"),new MenuType<>(ItemSlotsMenu::new,FeatureFlags.DEFAULT_FLAGS));
        FILTER_MENU=Registry.register(BuiltInRegistries.MENU,PowerMod.id("filter"),new MenuType<>(FilterMenu::new,FeatureFlags.DEFAULT_FLAGS));
        MENU=Registry.register(BuiltInRegistries.MENU,PowerMod.id("work_machine"),new MenuType<>(WorkMenu::new,FeatureFlags.DEFAULT_FLAGS));
        ItemStorage.SIDED.registerForBlockEntity((e,side)->e.itemStorage(side),ENTITY);
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((e,side)->e.fluidStorage(side),ENTITY);
        Processing.initialize();
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
                net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
                net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE,PowerMod.id("ore_black_quartz")));
    }
    public static Block block(String name,Function<BlockBehaviour.Properties,Block> factory,BlockBehaviour.Properties properties){
        var key=ResourceKey.create(Registries.BLOCK,PowerMod.id(name));
        var block=Registry.register(BuiltInRegistries.BLOCK,key,factory.apply(properties.setId(key)));
        BLOCKS.put(name,block);item(name,p->new BlockItem(block,p),new Item.Properties().useBlockDescriptionPrefix());return block;
    }
    public static void item(String name,Function<Item.Properties,Item> factory,Item.Properties properties){
        if(PowerMod.ITEMS.containsKey(name))throw new IllegalArgumentException("Duplicate item "+name);
        var key=ResourceKey.create(Registries.ITEM,PowerMod.id(name));
        PowerMod.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,factory.apply(properties.setId(key))));
    }
}
