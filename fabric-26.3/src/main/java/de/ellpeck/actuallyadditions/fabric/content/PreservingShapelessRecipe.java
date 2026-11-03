package de.ellpeck.actuallyadditions.fabric.content;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import java.util.List;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
public record PreservingShapelessRecipe(ShapelessRecipe delegate) implements CraftingRecipe {
    public static final RecipeSerializer<PreservingShapelessRecipe> SERIALIZER=new RecipeSerializer<>(ShapelessRecipe.MAP_CODEC.xmap(PreservingShapelessRecipe::new,PreservingShapelessRecipe::delegate),ShapelessRecipe.STREAM_CODEC.map(PreservingShapelessRecipe::new,PreservingShapelessRecipe::delegate));
    public static void register(){Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,PowerMod.id("copy_nbt_shapeless"),SERIALIZER);}
    @Override public boolean matches(CraftingInput input,Level level){return delegate.matches(input,level);}
    @Override public ItemStack assemble(CraftingInput input){var output=delegate.assemble(input);for(var stack:input.items())if(stack.getItem() instanceof EnergyItem){output.applyComponents(stack.getComponentsPatch());if(output.getItem() instanceof EnergyItem)EnergyItem.set(output,EnergyItem.energy(output));break;}return output;}
    @Override public RecipeSerializer<? extends CraftingRecipe> getSerializer(){return SERIALIZER;}
    @Override public CraftingBookCategory category(){return delegate.category();}
    @Override public boolean showNotification(){return delegate.showNotification();}
    @Override public String group(){return delegate.group();}
    @Override public PlacementInfo placementInfo(){return delegate.placementInfo();}
    @Override public List<RecipeDisplay> display(){return delegate.display();}
}
