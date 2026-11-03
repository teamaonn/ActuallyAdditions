package de.ellpeck.actuallyadditions.fabric.mixin;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.ellpeck.actuallyadditions.fabric.content.Lighting;
@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin {
 @Inject(method="calculateExplodedPositions",at=@At("RETURN"),cancellable=true)
 private void aa$protect(CallbackInfoReturnable<List<BlockPos>> cir){var explosion=(ServerExplosion)(Object)this;if(explosion.getBlockInteraction()!=Explosion.BlockInteraction.KEEP)cir.setReturnValue(Lighting.protect(explosion.level(),cir.getReturnValue()));}
}
