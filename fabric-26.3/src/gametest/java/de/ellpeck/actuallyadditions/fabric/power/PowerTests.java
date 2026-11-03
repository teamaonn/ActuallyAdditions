package de.ellpeck.actuallyadditions.fabric.power;

import java.util.*;
import exnihiloomnia.fabric.ExNihiloOmnia;
import net.blay09.mods.excompressum.block.ModBlocks;
import net.blay09.mods.excompressum.block.entity.AutoSieveBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;

public final class PowerTests {
    private static void check(boolean pass, String text) { if (!pass) throw new IllegalStateException(text); }
    private static GeneratorEntity generator(GameTestHelper h, int x, int y, int z) {
        var pos = h.absolutePos(new BlockPos(x, y, z)); h.getLevel().setBlock(pos, PowerMod.GENERATOR.defaultBlockState(), 3);
        return (GeneratorEntity) h.getLevel().getBlockEntity(pos);
    }
    private static RelayEntity relay(GameTestHelper h, int x, int y, int z) {
        var pos = h.absolutePos(new BlockPos(x, y, z)); h.getLevel().setBlock(pos, PowerMod.RELAYS.getFirst().defaultBlockState(), 3);
        return (RelayEntity) h.getLevel().getBlockEntity(pos);
    }
    private static AutoSieveBlockEntity sieve(GameTestHelper h, int x, int y, int z, boolean input) {
        var pos = h.absolutePos(new BlockPos(x, y, z)); h.getLevel().setBlock(pos, ModBlocks.autoSieve.asBlock().defaultBlockState(), 3);
        var e = (AutoSieveBlockEntity) h.getLevel().getBlockEntity(pos);
        e.getBackingContainer().setItem(21, new ItemStack(ExNihiloOmnia.ITEMS.get("mesh_diamond")));
        if (input) e.getBackingContainer().setItem(0, new ItemStack(Items.DIRT));
        return e;
    }
    private static void finished(AutoSieveBlockEntity sieve) {
        check(sieve.getCurrentStack().isEmpty() && sieve.getBackingContainer().getItem(0).isEmpty(), "Auto Sieve must complete its dirt input using generator power");
        int total = 0;
        for (int i = 1; i < 21; i++) total += sieve.getBackingContainer().getItem(i).getCount();
        check(total >= 2, "Auto Sieve must produce guaranteed pebbles");
        check(sieve.getMeshStack().getCount() == 1, "Mesh must remain installed");
    }
    @GameTest public void furnaceFuelBufferAndPersistence(GameTestHelper h) {
        var gen = generator(h, 1, 2, 1); gen.setItem(0, new ItemStack(Items.CHARCOAL, 2));
        int duration = GeneratorEntity.fuelTime(h.getLevel(), gen.getItem(0));
        check(duration == 1600, "Use real vanilla charcoal burn duration");
        gen.tick();
        check(gen.energy() == 20 && gen.burn() == duration - 1 && gen.getItem(0).getCount() == 1, "Consume one fuel and produce 20 energy");
        var copy = (GeneratorEntity)BlockEntity.loadStatic(gen.getBlockPos(), gen.getBlockState(), gen.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        copy.setLevel(h.getLevel());
        check(copy.energy() == gen.energy() && copy.burn() == gen.burn() && copy.getItem(0).getCount() == 1, "Saved fuel, energy and remaining burn survive reload");
        copy.tick(); check(copy.energy() == 40 && copy.getItem(0).getCount() == 1, "Reload must resume without consuming another fuel");
        var lava = generator(h, 4, 2, 1); lava.setItem(0, new ItemStack(Items.LAVA_BUCKET)); lava.tick();
        check(lava.getItem(0).is(Items.BUCKET), "Lava fuel must leave its bucket");
        check(lava.canTakeItemThroughFace(0, lava.getItem(0), Direction.DOWN), "Hopper can collect empty bucket");
        check(!gen.canTakeItemThroughFace(0, gen.getItem(0), Direction.DOWN), "Hopper must not steal fuel");
        h.succeed();
    }
    @GameTest public void fullBufferAndRedstonePreserveFuel(GameTestHelper h) {
        var gen = generator(h, 1, 2, 1); gen.setItem(0, new ItemStack(Items.COAL)); gen.storage.setEnergy(GeneratorEntity.CAPACITY);
        gen.tick(); check(gen.getItem(0).getCount() == 1 && gen.burn() == 0, "Full buffer does not consume fuel");
        gen.storage.setEnergy(0); h.getLevel().setBlock(gen.getBlockPos().east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        gen.tick(); check(gen.energy() == 0 && gen.getItem(0).getCount() == 1, "Redstone pauses generation without consuming fuel");
        h.getLevel().removeBlock(gen.getBlockPos().east(), false); gen.tick(); check(gen.energy() == 20, "Removing redstone resumes generation");
        h.succeed();
    }
    @GameTest public void relaySimulationLossAndSharedTickBudget(GameTestHelper h) {
        var a = relay(h, 1, 2, 1); var b = relay(h, 5, 2, 1); var sink = sieve(h, 6, 2, 1, false);
        RelayEntity.connect(a, b);
        check(a.storage.fill(5000, true) == 1000, "Simulation respects base relay throughput");
        check(a.storage.fill(5000, true) == 1000 && sink.getEnergyStored() == 0, "Simulation must not consume budget or change receiver");
        check(a.storage.fill(5000, false) == 1000 && sink.getEnergyStored() == 950, "1000 source energy yields 950 after 5 percent loss");
        check(a.storage.fill(5000, false) == 0, "Repeated calls cannot bypass same-tick throughput cap");
        h.succeed();
    }
    @GameTest public void cyclesModesAndReplacementCannotDuplicatePower(GameTestHelper h) {
        var a = relay(h, 1, 2, 1); var b = relay(h, 5, 2, 1); var c = relay(h, 3, 2, 2); var sink = sieve(h, 6, 2, 1, false);
        RelayEntity.connect(a, b); RelayEntity.connect(a, c); RelayEntity.connect(c, b);
        check(a.storage.fill(200, false) == 200 && sink.getEnergyStored() == 190, "Cycle must deliver once with loss");
        b.mode = RelayEntity.Mode.INPUT_ONLY;
        check(a.storage.fill(100, false) == 0, "Input-only receiving relay must not output to machines");
        b.mode = RelayEntity.Mode.BOTH; a.mode = RelayEntity.Mode.OUTPUT_ONLY;
        check(a.storage.fill(100, false) == 0, "Output-only source relay rejects generator input");
        a.mode = RelayEntity.Mode.BOTH; b.node = UUID.randomUUID();
        check(a.storage.fill(100, false) == 0, "Stale links must not connect to replacement relay identity");
        h.succeed();
    }
    @GameTest public void linksSurviveReloadAndRespectRange(GameTestHelper h) {
        var a = relay(h, 1, 2, 1); var b = relay(h, 5, 2, 1); RelayEntity.connect(a, b);
        var copy = (RelayEntity)BlockEntity.loadStatic(a.getBlockPos(), a.getBlockState(), a.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        check(copy.node.equals(a.node) && copy.links.equals(a.links), "Persistent identity and reciprocal link survive reload");
        var far = new RelayEntity(a.getBlockPos().east(16), PowerMod.RELAYS.getFirst().defaultBlockState()); far.setLevel(h.getLevel());
        RelayEntity.connect(a, far); check(!a.links.containsKey(far.getBlockPos()), "Reject links over 15 blocks");
        a.rangeUpgrade = true; RelayEntity.connect(a, far); check(!a.links.containsKey(far.getBlockPos()), "Both endpoints need range upgrade");
        far.rangeUpgrade = true; RelayEntity.connect(a, far); check(a.links.containsKey(far.getBlockPos()), "Range upgrade allows 16-block link");
        a.unlinkAll(); check(!b.links.containsKey(a.getBlockPos()), "Unlink removes reciprocal live connection");
        h.succeed();
    }
    @GameTest public void wrenchLinksUsingNormalBlockInteraction(GameTestHelper h) {
        var a = relay(h, 1, 2, 1); var b = relay(h, 5, 2, 1);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(PowerMod.ITEMS.get("laser_wrench")); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var first = new BlockHitResult(Vec3.atCenterOf(a.getBlockPos()), Direction.UP, a.getBlockPos(), false);
        var result = a.getBlockState().useItemOn(stack, h.getLevel(), player, InteractionHand.MAIN_HAND, first);
        check(!result.consumesAction(), "Relay block must allow wrench item interaction instead of opening its status");
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, first));
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(b.getBlockPos()), Direction.UP, b.getBlockPos(), false)));
        check(a.links.get(b.getBlockPos()).equals(b.node), "Wrench connects selected relays");
        h.succeed();
    }
    @GameTest(maxTicks = 400) public void generatorPowersOriginalAutoSieveDirectly(GameTestHelper h) {
        var gen = generator(h, 2, 2, 1); var sink = sieve(h, 3, 2, 1, true); gen.setItem(0, new ItemStack(Items.COAL));
        h.runAfterDelay(330, () -> { finished(sink); check(gen.getItem(0).isEmpty(), "Generator consumed its coal"); h.succeed(); });
    }
    @GameTest(maxTicks = 260) public void generatorsPowerOriginalAutoSieveWirelessly(GameTestHelper h) {
        var a = relay(h, 2, 2, 1); var b = relay(h, 5, 2, 1); var sink = sieve(h, 6, 2, 1, true); RelayEntity.connect(a, b);
        for (var p : List.of(new BlockPos(1, 2, 1), new BlockPos(2, 2, 0), new BlockPos(2, 2, 2))) generator(h, p.getX(), p.getY(), p.getZ()).setItem(0, new ItemStack(Items.COAL));
        h.runAfterDelay(210, () -> { finished(sink); h.succeed(); });
    }
}
