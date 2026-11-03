package de.ellpeck.actuallyadditions.fabric.mixin;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class GogglesMixin {
 @Inject(method="shouldEntityAppearGlowing",at=@At("HEAD"),cancellable=true)
 private void aa$outline(Entity entity,CallbackInfoReturnable<Boolean> cir){var p=Minecraft.getInstance().player;if(p!=null&&entity instanceof Mob&&p.distanceToSqr(entity)<=1024&&p.getItemBySlot(EquipmentSlot.HEAD).is(PowerMod.ITEMS.get("engineers_goggles_advanced")))cir.setReturnValue(true);}
}
