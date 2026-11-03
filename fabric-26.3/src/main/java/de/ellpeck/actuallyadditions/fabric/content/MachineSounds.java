package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.*;
public final class MachineSounds {
 private static final Map<String,SoundEvent> EVENTS=new HashMap<>();
 public static void initialize(){for(var name:List.of("coffee_machine","crusher","reconstructor","duh_duh_duh_duuuh"))EVENTS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,PowerMod.id(name),SoundEvent.createVariableRangeEvent(PowerMod.id(name))));}
 public static void play(WorkEntity machine,String name,float volume){machine.getLevel().playSound(null,machine.getBlockPos(),EVENTS.get(name),SoundSource.BLOCKS,volume,1F);}
}
