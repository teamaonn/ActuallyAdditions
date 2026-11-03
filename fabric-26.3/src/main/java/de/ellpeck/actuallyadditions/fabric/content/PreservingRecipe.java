package de.ellpeck.actuallyadditions.fabric.content;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import java.util.List;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;

/** Native 26.3 recipe serializer preserving the charged target's data component patch. */
public record PreservingRecipe(ShapedRecipe delegate) implements CraftingRecipe {
    public static final RecipeSerializer<PreservingRecipe> SERIALIZER=new RecipeSerializer<>(ShapedRecipe.MAP_CODEC.xmap(PreservingRecipe::new,PreservingRecipe::delegate),ShapedRecipe.STREAM_CODEC.map(PreservingRecipe::new,PreservingRecipe::delegate));
    public static void register(){Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,PowerMod.id("copy_nbt"),SERIALIZER);}
    @Override public boolean matches(CraftingInput input,Level level){return delegate.matches(input,level);}
    @Override public ItemStack assemble(CraftingInput input){
        var output=delegate.assemble(input);
        for(var stack:input.items())if(stack.getItem() instanceof EnergyItem){output.applyComponents(stack.getComponentsPatch());if(output.getItem() instanceof EnergyItem)EnergyItem.set(output,EnergyItem.energy(output));break;}
        return output;
    }
    @Override public RecipeSerializer<? extends CraftingRecipe> getSerializer(){return SERIALIZER;}
    @Override public CraftingBookCategory category(){return delegate.category();}
    @Override public boolean showNotification(){return delegate.showNotification();}
    @Override public String group(){return delegate.group();}
    @Override public PlacementInfo placementInfo(){return delegate.placementInfo();}
    @Override public List<RecipeDisplay> display(){return delegate.display();}
}
