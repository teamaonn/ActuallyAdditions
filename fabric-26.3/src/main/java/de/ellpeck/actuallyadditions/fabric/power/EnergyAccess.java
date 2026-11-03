package de.ellpeck.actuallyadditions.fabric.power;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.platform.capabilities.CommonCapabilities;
import net.blay09.mods.balm.platform.energy.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;

final class EnergyAccess {
    private EnergyAccess() {}
    static EnergyStorage at(Level level, BlockPos pos, Direction side) {
        // Never ask for an unloaded block entity: power networks do not load chunks.
        if (!level.hasChunkAt(pos)) return null;
        var entity = level.getBlockEntity(pos);
        if (entity instanceof BalmEnergyStorageProvider provider) return provider.getEnergyStorage(side);
        if (entity == null || CommonCapabilities.ENERGY_STORAGE == null) return null;
        return Balm.capabilities().getCapability(entity, side, CommonCapabilities.ENERGY_STORAGE);
    }
}
