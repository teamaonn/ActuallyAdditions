package de.ellpeck.actuallyadditions.fabric.mixin;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(StructureProcessorList.class)
public interface ProcessorAccess {
 @Accessor("list") @Mutable void aa$list(List<StructureProcessor> value);
}
