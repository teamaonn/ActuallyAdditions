package de.ellpeck.actuallyadditions.fabric.mixin;
import java.util.List;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(StructureTemplatePool.class)
public interface PoolAccess {
 @Accessor("templates") ObjectArrayList<StructurePoolElement> aa$templates();
 @Accessor("rawTemplates") @Mutable void aa$rawTemplates(List<Pair<StructurePoolElement,Integer>> value);
}
