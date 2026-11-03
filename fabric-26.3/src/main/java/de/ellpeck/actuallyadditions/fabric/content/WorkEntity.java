package de.ellpeck.actuallyadditions.fabric.content;

import java.util.*;
import com.google.gson.*;
import de.ellpeck.actuallyadditions.fabric.power.PowerBlock;
import net.blay09.mods.balm.platform.energy.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

public final class WorkEntity extends BlockEntity implements WorldlyContainer,MenuProvider,BalmEnergyStorageProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(12,ItemStack.EMPTY);
    private final List<ItemStack>[] pending=new List[]{new ArrayList<>(),new ArrayList<>()};
    private final ItemStack[] jobInput={ItemStack.EMPTY,ItemStack.EMPTY};
    private final String[] jobKey={"",""};
    private final int[] progress={0,0};
    private int energy,counter,burn,production,heatAge;
    private long experience;
    private int farmIndex,digIndex,digY=Integer.MIN_VALUE;
    public boolean onlyOres;
    public java.util.UUID playerId;
    public BlockPos bound;
    public final Lighting.State lighting=new Lighting.State(this);
    public final Transport.Node network=new Transport.Node(this);
    private int coffeeCache;public boolean brewing;
    public final int[] weakSignal=new int[6],strongSignal=new int[6];
    public final MachineTank inputTank=new MachineTank(this,true), outputTank=new MachineTank(this,false);
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> fluidStorage(){
        return switch(kind()){case PRESS,FLUID_COLLECTOR->outputTank;case OIL,FLUID_PLACER,COFFEE->inputTank;case FERMENT->new net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage<>(List.of(inputTank,outputTank));default->null;};
    }
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> fluidStorage(Direction side){return kind()==WorkKind.FLUID_RELAY?Transport.fluids(this,side):kind()==WorkKind.PHANTOM_FLUID?new RemoteStorage<>(()->remoteFluid(side),()->boundAvailable()?level.getBlockEntity(bound):null):fluidStorage();}
    public boolean isTransportRelay(){return kind()==WorkKind.ITEM_RELAY||kind()==WorkKind.ADVANCED_ITEM_RELAY||kind()==WorkKind.FLUID_RELAY;}
    public net.minecraft.server.level.ServerPlayer linkedPlayer(){if(playerId==null||!(level instanceof ServerLevel server))return null;var p=server.getServer().getPlayerList().getPlayer(playerId);int range=upgradedRange(32);return p!=null&&p.level()==level&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=(long)range*range?p:null;}
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> itemStorage(Direction side){return switch(kind()){
        case PHANTOM_ITEM->new RemoteStorage<>(()->remoteItems(side),()->boundAvailable()?level.getBlockEntity(bound):null);
        case ITEM_INTERFACE,HOPPING_INTERFACE->Transport.items(this);
        case PLAYER_INTERFACE->new RemoteStorage<>(()->{var p=linkedPlayer();return p==null?null:net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(p.getInventory(),side);},this::linkedPlayer);
        case ITEM_RELAY,ADVANCED_ITEM_RELAY,FLUID_RELAY->null;
        default->net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(this,side);
    };}
    public boolean isPhantom(){return switch(kind()){case PHANTOM_ITEM,PHANTOM_FLUID,PHANTOM_ENERGY,PHANTOM_REDSTONE,PHANTOM_BREAKER,PHANTOM_PLACER->true;default->false;};}
    public boolean phantomWork(){return kind()==WorkKind.PHANTOM_BREAKER||kind()==WorkKind.PHANTOM_PLACER;}
    public boolean boundAvailable(){return bound!=null&&level!=null&&level.hasChunkAt(bound)&&worldPosition.distSqr(bound)<=(long)upgradedRange(phantomWork()?3:16)*upgradedRange(phantomWork()?3:16)&&!(level.getBlockEntity(bound) instanceof WorkEntity e&&e.isPhantom());}
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> remoteItems(Direction side){return boundAvailable()?net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level,bound,side):null;}
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> remoteFluid(Direction side){return boundAvailable()?net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level,bound,side):null;}
    @Override public EnergyStorage getEnergyStorage(Direction side){
        if(kind()!=WorkKind.PHANTOM_ENERGY)return storage;
        if(!boundAvailable()||level.getBlockEntity(bound) instanceof de.ellpeck.actuallyadditions.fabric.power.RelayEntity)return null;
        return de.ellpeck.actuallyadditions.fabric.power.EnergyAccess.at(level,bound,side);
    }
    public boolean pulseMode;
    private boolean lastSignal;
    public WorkEntity(BlockPos pos,BlockState state){super(Content.ENTITY,pos,state);}
    public WorkKind kind(){return ((WorkBlock)getBlockState().getBlock()).kind;}
    public int energy(){return kind()==WorkKind.BATTERY_BOX?EnergyItem.energy(items.getFirst()):energy;}
    public int progress(int lane){return progress[lane];}
    public int capacity(){return kind()==WorkKind.BATTERY_BOX?(items.getFirst().getItem() instanceof EnergyItem item?item.capacity:0):kind().capacity;}
    private long receiveTick=Long.MIN_VALUE;
    private int received,extracted;
    private int receiveRemaining(){long time=level==null?0:level.getGameTime();if(receiveTick!=time){receiveTick=time;received=0;extracted=0;}return Math.max(0,Math.min(kind().inputRate,kind()==WorkKind.BATTERY_BOX&&items.getFirst().getItem() instanceof EnergyItem item?item.rate:kind().inputRate)-received);}
    private final EnergyStorage storage=new EnergyStorage(){
        public int fill(int amount,boolean simulate){if(!canFill())return 0;int n=Math.max(0,Math.min(amount,Math.min(capacity()-energy(),receiveRemaining())));if(kind()==WorkKind.BATTERY_BOX)n=EnergyItem.insert(items.getFirst(),n,simulate);if(!simulate&&n>0){if(kind()!=WorkKind.BATTERY_BOX)energy+=n;received+=n;setChanged();}return n;}
        public int drain(int amount,boolean simulate){if(!canDrain())return 0;receiveRemaining();int rate=kind()==WorkKind.BATTERY_BOX&&items.getFirst().getItem() instanceof EnergyItem item?item.rate:outputRate();int n=Math.max(0,Math.min(amount,Math.min(energy(),rate-extracted)));if(kind()==WorkKind.BATTERY_BOX)n=EnergyItem.extract(items.getFirst(),n,simulate);if(!simulate&&n>0){if(kind()!=WorkKind.BATTERY_BOX)energy-=n;extracted+=n;setChanged();}return n;}
        public int getEnergy(){return energy();}
        public void setEnergy(int value){if(kind()==WorkKind.BATTERY_BOX)EnergyItem.set(items.getFirst(),value);else energy=Math.clamp(value,0,capacity());setChanged();}
        public int getCapacity(){return capacity();}
        public boolean canFill(){return kind()!=WorkKind.ENERVATOR&&!generates()&&capacity()>0;}
        public boolean canDrain(){return kind()==WorkKind.ENERVATOR||kind()==WorkKind.BATTERY_BOX||generates();}
    };
    @Override public EnergyStorage getEnergyStorage(){return storage;}
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        boolean signal=level.hasNeighborSignal(worldPosition),active=false;
        switch(kind()){
            case PHANTOM_ITEM,PHANTOM_FLUID -> {}
            case DISPLAY -> {if(energy>=60&&items.getFirst().getItem() instanceof Accessories.Blower blower&&blower.clear(server,worldPosition,(int)(level.getGameTime()%Integer.MAX_VALUE),net.fabricmc.fabric.api.entity.FakePlayer.get(server))){energy-=60;setChanged();active=true;}}
            case SHOCK,LAMP_CONTROLLER,FIREWORK -> {active=lighting.tick(signal);}
            case ITEM_RELAY,ADVANCED_ITEM_RELAY,FLUID_RELAY,ITEM_INTERFACE,HOPPING_INTERFACE -> Transport.tick(this);
            case PLAYER_INTERFACE -> {var p=linkedPlayer();if(p!=null)for(int i=0;i<p.getInventory().getContainerSize()&&energy>0;i++){int n=EnergyItem.insert(p.getInventory().getItem(i),energy,false);if(n>0){energy-=n;setChanged();active=true;}}}
            case COFFEE -> {if(signal)brewing=true;active=coffee();}
            case PHANTOM_ENERGY -> {if(boundAvailable())for(var side:Direction.values()){
                var source=getEnergyStorage(side);if(source==null||!source.canDrain())continue;var pos=worldPosition.relative(side);if(!level.hasChunkAt(pos))continue;var target=de.ellpeck.actuallyadditions.fabric.power.EnergyAccess.at(level,pos,side.getOpposite());if(target==null||!target.canFill())continue;int offer=source.drain(Integer.MAX_VALUE,true);if(offer>0){int n=Math.clamp(target.fill(offer,false),0,offer);source.drain(n,false);active=n>0||active;}
            }}
            case PHANTOM_REDSTONE -> {boolean changed=false;for(var side:Direction.values()){int i=side.ordinal(),weak=boundAvailable()?level.getBlockState(bound).getSignal(level,bound,side):0,strong=boundAvailable()?level.getBlockState(bound).getDirectSignal(level,bound,side):0;if(weak!=weakSignal[i]||strong!=strongSignal[i]){weakSignal[i]=weak;strongSignal[i]=strong;changed=true;}}if(changed)level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}
            case PHANTOM_BREAKER,PHANTOM_PLACER -> {if(pulseMode?signal&&!lastSignal:!signal&&++counter>=15){counter=0;active=worldWork();setChanged();}}
            case LAVA -> {if(!signal)active=lava();}
            case DIGGER -> {if(!signal&&++counter>=5){counter=0;active=dig();setChanged();}}
            case FLUID_COLLECTOR,FLUID_PLACER -> {if(pulseMode?signal&&!lastSignal:!signal&&++counter>=15){counter=0;active=fluidWorld();setChanged();}if(kind()==WorkKind.FLUID_COLLECTOR)pushFluid();}
            case COLLECTOR -> {if(pulseMode?signal&&!lastSignal:!signal)active=collect();}
            case FARMER -> {if(!signal&&++counter>=5){counter=0;active=farm();setChanged();}}
            case FEEDER -> {if(!signal)active=feed();}
            case XP -> {active=solidify(!signal);}
            case OIL -> {active=oil(!signal);pushEnergy();}
            case PRESS,FERMENT -> {if(!signal)active=fluidProcess();pushFluid();}
            case HEAT,LEAF,BIO -> {active=generate(!signal);pushEnergy();}
            case BREAKER,PLACER,DROPPER,LONG_BREAKER -> {
                if(pulseMode){if(signal&&!lastSignal)active=worldWork();}
                else if(!signal&&++counter>=(kind()==WorkKind.DROPPER?5:15)){counter=0;active=worldWork();setChanged();}
            }
            case ENERGIZER,ENERVATOR,BATTERY_BOX -> {if(!signal)active=station();}
            case RECONSTRUCTOR -> {
                if(pulseMode){if(signal&&!lastSignal)active=fire();}
                else if(!signal&&++counter>=100){counter=0;active=fire();}
            }
            case EMPOWERER -> {if(!signal)active=empower();}
            case FURNACE,CRUSHER,DOUBLE_CRUSHER -> {
                flush(0);flush(1);
                if(!signal){active=process(0);if(kind()==WorkKind.FURNACE||kind()==WorkKind.DOUBLE_CRUSHER)active=process(1)||active;}
            }
        }
        lastSignal=signal;
        if(getBlockState().getValue(PowerBlock.ACTIVE)!=active)level.setBlock(worldPosition,getBlockState().setValue(PowerBlock.ACTIVE,active),3);
    }
    private boolean station(){
        var stack=items.getFirst();boolean active=false;
        if(kind()==WorkKind.ENERGIZER&&stack.getItem() instanceof EnergyItem item){
            int n=EnergyItem.insert(stack,Math.min(1000,energy),false);energy-=n;active=n>0;
            if(EnergyItem.energy(stack)==item.capacity&&items.get(1).isEmpty()){items.set(1,stack);items.set(0,ItemStack.EMPTY);active=true;}
        }else if(kind()==WorkKind.ENERVATOR&&stack.getItem() instanceof EnergyItem){
            int n=EnergyItem.extract(stack,Math.min(1000,capacity()-energy),false);energy+=n;active=n>0;
            if(EnergyItem.energy(stack)==0&&items.get(1).isEmpty()){items.set(1,stack);items.set(0,ItemStack.EMPTY);active=true;}
        }
        if(kind()==WorkKind.ENERVATOR||kind()==WorkKind.BATTERY_BOX&&EnergyItem.enabled(stack))active=pushEnergy()||active;
        if(active)setChanged();return active;
    }
    private boolean coffee(){
        var beans=items.getFirst();if(coffeeCache<=298&&beans.is(de.ellpeck.actuallyadditions.fabric.power.PowerMod.ITEMS.get("coffee_beans"))){beans.shrink(1);coffeeCache+=2;setChanged();}
        if(!brewing)return false;
        if(!items.get(1).is(de.ellpeck.actuallyadditions.fabric.power.PowerMod.ITEMS.get("empty_cup"))||!items.get(2).isEmpty()||coffeeCache<35||inputTank.variant.getFluid()!=net.minecraft.world.level.material.Fluids.WATER||inputTank.amount<500L*81){progress[0]=0;brewing=false;setChanged();return false;}
        if(energy<150)return false;if(progress[0]%30==0)MachineSounds.play(this,"coffee_machine",.1F);energy-=150;progress[0]++;setChanged();
        if(progress[0]>=500){
            var ingredients=new ArrayList<>(items.subList(3,11));var output=Coffee.brew(level,ingredients);
            try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){if(inputTank.take(inputTank.variant,500L*81,tx)!=500L*81)return false;tx.commit();}
            for(int i=0;i<8;i++)items.set(i+3,ingredients.get(i));items.get(1).shrink(1);items.set(2,output);coffeeCache-=35;progress[0]=0;brewing=false;
        }return true;
    }
    public int upgradedRange(int base){
        for(int i=1;i<=3;i++){var p=worldPosition.above(i);if(!level.hasChunkAt(p)||!level.getBlockState(p).is(Content.BLOCKS.get("phantom_booster")))break;base*=2;}return base;
    }
    private boolean lava(){
        var center=worldPosition.above();
        if(energy<150000||!level.isEmptyBlock(center)){progress[0]=0;return false;}
        for(var side:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST))if(!level.getBlockState(center.relative(side)).is(Content.BLOCKS.get("lava_factory_casing"))){progress[0]=0;return false;}
        if(++progress[0]>=200){if(level.setBlockAndUpdate(center,Blocks.LAVA.defaultBlockState()))energy-=150000;progress[0]=0;}setChanged();return true;
    }
    public boolean fluidWorld(){
        var pos=worldPosition.relative(getBlockState().getValue(PowerBlock.FACING));if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);
        if(kind()==WorkKind.FLUID_COLLECTOR){
            var fluid=state.getFluidState().getType();boolean cauldron=false;
            if(state.getBlock() instanceof AbstractCauldronBlock c&&c.isFull(state)){
                if(state.is(Blocks.WATER_CAULDRON)){fluid=net.minecraft.world.level.material.Fluids.WATER;cauldron=true;}
                else if(state.is(Blocks.LAVA_CAULDRON)){fluid=net.minecraft.world.level.material.Fluids.LAVA;cauldron=true;}
            }
            if(!cauldron&&(!(state.getBlock() instanceof LiquidBlock)||!state.getFluidState().isSource()))return false;
            var variant=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid);
            try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                if(outputTank.add(variant,81000,tx)!=81000)return false;
                if(!level.setBlockAndUpdate(pos,cauldron?Blocks.CAULDRON.defaultBlockState():Blocks.AIR.defaultBlockState()))return false;tx.commit();return true;
            }
        }
        if(inputTank.amount<81000||!state.canBeReplaced()||state.getFluidState().isSource()||level.getBlockEntity(pos)!=null)return false;
        var fluid=inputTank.variant.getFluid();var block=fluid.defaultFluidState().createLegacyBlock();if(block.isAir())return false;
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            if(inputTank.take(inputTank.variant,81000,tx)!=81000)return false;
            if(fluid.is(net.minecraft.tags.FluidTags.WATER)&&level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,pos))((ServerLevel)level).sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,8,.4,.4,.4,0);
            else if(!level.setBlockAndUpdate(pos,block))return false;tx.commit();return true;
        }
    }
    public boolean dig(){
        if(digY==Integer.MIN_VALUE)digY=worldPosition.getY()-1;if(digY<level.getMinY())return false;
        int range=upgradedRange(2),width=range*2+1;digIndex=Math.floorMod(digIndex,width*width);
        var pos=new BlockPos(worldPosition.getX()+digIndex%width-range,digY,worldPosition.getZ()+digIndex/width-range);
        if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);int cost=650*(onlyOres?3:1);boolean mined=false;
        String blockId=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();boolean denied=PortConfig.contains("machineSettings","verticalDiggerBlacklist",blockId);
        boolean ore=PortConfig.contains("machineSettings","verticalDiggerExtraWhitelist",blockId)||state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.parse("c:ores")));
        if(!state.isAir()&&!denied&&(!onlyOres||ore)&&state.getDestroySpeed(level,pos)>=0){
            if(energy<cost)return false;if(harvest(pos,state)){energy-=cost;mined=true;}
        }
        if(++digIndex>=width*width){digIndex=0;digY--;}setChanged();return mined;
    }
    public boolean collect(){
        boolean active=false;
        for(var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(worldPosition).inflate(6))){
            var stack=entity.getItem();if(!entity.isAlive()||entity.hasPickUpDelay()||stack.isEmpty()||!FilterItem.accepts(items.get(6),stack))continue;
            var copy=new ArrayList<>(items.stream().map(ItemStack::copy).toList());if(!insertRange(copy,stack,0,6))continue;
            for(int i=0;i<6;i++)items.set(i,copy.get(i));entity.discard();active=true;
        }if(active)setChanged();return active;
    }
    private static boolean seed(ItemStack stack){
        if(stack.isEmpty())return false;
        return stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.parse("c:seeds")))||stack.getItem() instanceof BlockItem item&&(item.getBlock() instanceof CropBlock||item.getBlock() instanceof StemBlock||item.getBlock() instanceof NetherWartBlock||item.getBlock() instanceof SugarCaneBlock||item.getBlock() instanceof CactusBlock);
    }
    private static boolean insertRange(List<ItemStack> inventory,ItemStack source,int start,int end){
        var stack=source.copy();
        for(int i=start;i<end&&!stack.isEmpty();i++){var target=inventory.get(i);if(!target.isEmpty()&&ItemStack.isSameItemSameComponents(stack,target)){int n=Math.min(stack.getCount(),target.getMaxStackSize()-target.getCount());target.grow(n);stack.shrink(n);}}
        for(int i=start;i<end&&!stack.isEmpty();i++)if(inventory.get(i).isEmpty())inventory.set(i,stack.split(Math.min(stack.getCount(),stack.getMaxStackSize())));
        return stack.isEmpty();
    }
    public boolean farm(){
        int area=PortConfig.area("farmerArea"),radius=area/2;int index=Math.floorMod(farmIndex,area*area);farmIndex=(index+1)%(area*area);
        var side=getBlockState().getValue(PowerBlock.FACING);var center=worldPosition.relative(side,radius+1);var pos=center.offset(index%area-radius,0,index/area-radius);
        if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);var block=state.getBlock();
        boolean crop=block instanceof CropBlock c&&c.isMaxAge(state),wart=block instanceof NetherWartBlock&&state.getValue(NetherWartBlock.AGE)==3;
        boolean fruit=block==Blocks.MELON||block==Blocks.PUMPKIN;
        boolean stalk=block instanceof SugarCaneBlock||block instanceof CactusBlock;
        if(crop||wart||fruit||stalk){
            var harvest=pos;if(stalk){harvest=pos.above();if(!level.getBlockState(harvest).is(block))return false;state=level.getBlockState(harvest);}
            int cost=wart?500:250;if(energy<cost||level.getBlockEntity(harvest)!=null)return false;
            var fake=net.fabricmc.fabric.api.entity.FakePlayer.get((ServerLevel)level);
            if(!net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,fake,harvest,state,null))return false;
            var copy=new ArrayList<>(items.stream().map(ItemStack::copy).toList());var drops=Block.getDrops(state,(ServerLevel)level,harvest,null);
            if(drops.isEmpty())return false;
            for(var drop:drops){var before=new ArrayList<>(copy.stream().map(ItemStack::copy).toList());if(seed(drop)&&insertRange(copy,drop,0,6))continue;copy=before;if(!insertRange(copy,drop,6,12))return false;}
            if(!level.setBlockAndUpdate(harvest,Blocks.AIR.defaultBlockState()))return false;
            for(int i=0;i<12;i++)items.set(i,copy.get(i));energy-=cost;setChanged();return true;
        }
        if(!state.canBeReplaced())return false;
        for(int i=0;i<6;i++){
            var stack=items.get(i);if(!seed(stack)||!(stack.getItem() instanceof BlockItem item))continue;
            var plant=item.getBlock().defaultBlockState();var soil=pos.below();boolean till=plant.getBlock() instanceof CropBlock&&(level.getBlockState(soil).is(Blocks.DIRT)||level.getBlockState(soil).is(Blocks.GRASS_BLOCK));
            int cost=plant.getBlock() instanceof NetherWartBlock?500:plant.getBlock() instanceof CactusBlock||plant.getBlock() instanceof SugarCaneBlock?250:350;
            if(energy<cost*(till?2:1))return false;
            if(till){if(!Blocks.FARMLAND.defaultBlockState().canSurvive(level,soil))continue;level.setBlockAndUpdate(soil,Blocks.FARMLAND.defaultBlockState());energy-=cost;setChanged();}
            if(!plant.canSurvive(level,pos))continue;
            if(level.setBlockAndUpdate(pos,plant)){stack.shrink(1);energy-=cost;setChanged();return true;}
        }return false;
    }
    private boolean feed(){
        counter=Math.min(100,counter+1);var stack=items.getFirst();if(counter<100||stack.isEmpty())return false;
        var animals=level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,new AABB(worldPosition).inflate(5));if(animals.size()<2||animals.size()>=30)return false;
        for(var animal:animals)if(animal.getAge()==0&&!animal.isInLove()&&animal.canFallInLove()&&animal.isFood(stack)){
            animal.setInLove(null);stack.shrink(1);counter=0;setChanged();return true;
        }return false;
    }
    private boolean solidify(boolean enabled){
        boolean active=false;
        if(enabled)for(var orb:level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,new AABB(worldPosition).inflate(5))){
            if(!orb.isAlive())continue;experience=Math.min(Integer.MAX_VALUE,experience+orb.getValue());orb.discard();active=true;
        }
        var source=items.get(1);var xp=de.ellpeck.actuallyadditions.fabric.power.PowerMod.ITEMS.get("solidified_experience");
        if(source.is(xp)&&experience<=Integer.MAX_VALUE-(long)source.getCount()*8){experience+=(long)source.getCount()*8;items.set(1,ItemStack.EMPTY);active=true;}
        var output=items.getFirst();if(experience>=8&&(output.isEmpty()||output.is(xp)&&output.getCount()<output.getMaxStackSize())){
            int n=(int)Math.min(experience/8,output.isEmpty()?64:output.getMaxStackSize()-output.getCount());if(output.isEmpty())items.set(0,new ItemStack(xp,n));else output.grow(n);experience-=n*8L;active=true;
        }
        if(active)setChanged();return active;
    }
    private static net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid(JsonObject definition){return net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(net.minecraft.resources.Identifier.parse(definition.get("id").getAsString())));}
    private static long volume(JsonObject definition){return Math.max(0,definition.get("amount").getAsLong())*81;}
    public boolean acceptsFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid){
        if(kind()==WorkKind.COFFEE)return fluid.getFluid()==net.minecraft.world.level.material.Fluids.WATER;if(kind()==WorkKind.FLUID_PLACER)return true;if(level==null)return false;
        String type=kind()==WorkKind.OIL?"liquid_fuel":"fermenting",key=kind()==WorkKind.OIL?"fuel":"ingredient";
        return Processing.all(level,type).stream().anyMatch(r->fluid(r.data().getAsJsonObject(key)).equals(fluid));
    }
    private boolean fluidProcess(){
        boolean press=kind()==WorkKind.PRESS;
        for(var recipe:Processing.all(level,press?"pressing":"fermenting")){
            var in=press?null:recipe.data().getAsJsonObject("ingredient");var out=recipe.data().getAsJsonObject(press?"fluid":"result");var result=fluid(out);
            if(press?!Processing.matches(recipe.data().get("ingredient"),items.getFirst()):!inputTank.variant.equals(fluid(in))||inputTank.amount<volume(in))continue;
            String key=recipe.key()+(press?items.getFirst().getComponents().hashCode():inputTank.variant.toString());
            if(!key.equals(jobKey[0])){reset(0);jobKey[0]=key;}
            if(!outputTank.isResourceBlank()&&!outputTank.variant.equals(result)||outputTank.amount+volume(out)>outputTank.getCapacity()||press&&energy<35)return false;
            if(press)energy-=35;progress[0]++;setChanged();
            if(progress[0]>= (press?30:Math.max(1,recipe.number("time",100)))){
                try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                    if(outputTank.add(result,volume(out),tx)!=volume(out))return false;
                    if(!press&&inputTank.take(fluid(in),volume(in),tx)!=volume(in))return false;
                    tx.commit();
                }
                if(press)items.getFirst().shrink(1);reset(0);
            }return true;
        }reset(0);return false;
    }
    private boolean oil(boolean enabled){
        if(burn>0){if(energy+production<=capacity()){energy+=production;burn--;setChanged();return true;}return false;}
        if(!enabled||energy>=capacity())return false;
        for(var recipe:Processing.all(level,"liquid_fuel")){
            var fuel=recipe.data().getAsJsonObject("fuel");if(!inputTank.variant.equals(fluid(fuel))||inputTank.amount<volume(fuel))continue;
            try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                if(inputTank.take(fluid(fuel),volume(fuel),tx)!=volume(fuel))continue;tx.commit();
            }
            burn=Math.max(1,recipe.number("burn_time",100));production=recipe.number("total_energy",4000)/burn;setChanged();return true;
        }return false;
    }
    private void pushFluid(){
        if(outputTank.amount<=0)return;
        for(var side:Direction.values()){
            var pos=worldPosition.relative(side);if(!level.hasChunkAt(pos))continue;
            var target=net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level,pos,side.getOpposite());if(target==null)continue;
            try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(outputTank,target,r->true,outputTank.amount,tx);tx.commit();
            }
            if(outputTank.amount<=0)break;
        }
    }
    public boolean generates(){return kind()==WorkKind.HEAT||kind()==WorkKind.LEAF||kind()==WorkKind.BIO||kind()==WorkKind.OIL;}
    private int outputRate(){return switch(kind()){case HEAT->80;case LEAF->450;case BIO->800;case OIL->PortConfig.number("oilGeneratorTransfer",100,Integer.MAX_VALUE);default->1000;};}
    private boolean pushEnergy(){
        boolean active=false;
        for(var side:Direction.values()){
            var pos=worldPosition.relative(side);if(!level.hasChunkAt(pos))continue;
            if(level.getBlockEntity(pos) instanceof WorkEntity target&&(target.generates()||target.kind()==WorkKind.BATTERY_BOX&&EnergyItem.enabled(target.items.getFirst())))continue;
            var target=de.ellpeck.actuallyadditions.fabric.power.EnergyAccess.at(level,pos,side.getOpposite());if(target==null||!target.canFill())continue;
            int offer=storage.drain(100000,true);if(offer<=0)break;
            int sent=Math.clamp(target.fill(offer,false),0,offer);if(sent>0){storage.drain(sent,false);active=true;}
        }return active;
    }
    public static boolean bioFuel(ItemStack stack){return stack.has(net.minecraft.core.component.DataComponents.FOOD)||stack.getItem() instanceof BlockItem b&&b.getBlock() instanceof BonemealableBlock;}
    private boolean generate(boolean enabled){
        boolean active=false;
        if(kind()==WorkKind.BIO){
            if(burn>0){if(energy+production<=capacity()){energy+=production;burn--;active=true;}}
            else if(enabled&&energy<capacity()){
                Set<Item> types=new HashSet<>();
                for(int i=0;i<8;i++){var stack=items.get(i);if(bioFuel(stack)&&types.add(stack.getItem()))stack.shrink(1);}
                if(!types.isEmpty()){production=(int)Math.pow(types.size()*2,2);burn=200-(int)Math.pow(1.8,types.size());active=true;}
            }
        }else if(enabled&&kind()==WorkKind.HEAT&&energy+40<=capacity()){
            List<BlockPos> heat=new ArrayList<>();
            for(var side:Direction.values())if(side!=Direction.UP){var p=worldPosition.relative(side);if(level.hasChunkAt(p)&&(level.getFluidState(p).is(net.minecraft.tags.FluidTags.LAVA)||level.getBlockState(p).getBlock() instanceof MagmaBlock))heat.add(p);}
            if(heat.size()>=4){energy+=40;active=true;if(++heatAge>=1000){heatAge=0;if(level.getRandom().nextInt(200)==0)level.setBlockAndUpdate(heat.get(level.getRandom().nextInt(heat.size())),Blocks.AIR.defaultBlockState());}}
        }else if(enabled&&kind()==WorkKind.LEAF&&++counter>=PortConfig.number("leafGeneratorCooldown",1,Integer.MAX_VALUE)){
            counter=0;int production=PortConfig.number("leafGeneratorCPPerLeaf",1,Integer.MAX_VALUE);int radius=PortConfig.area("leafGeneratorArea")/2;
            if((long)energy+production<=capacity()){
                List<BlockPos> leaves=new ArrayList<>();
                for(var p:BlockPos.betweenClosed(worldPosition.offset(-radius,-radius,-radius),worldPosition.offset(radius,radius,radius)))if(level.hasChunkAt(p)&&level.getBlockState(p).is(net.minecraft.tags.BlockTags.LEAVES))leaves.add(p.immutable());
                if(!leaves.isEmpty()){var p=leaves.get(level.getRandom().nextInt(leaves.size()));level.levelEvent(2001,p,Block.getId(level.getBlockState(p)));level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());energy+=production;active=true;}
            }
        }
        if(active)setChanged();return active;
    }
    private boolean insertAll(List<ItemStack> drops,boolean simulate){
        var copy=new ArrayList<>(items.stream().map(ItemStack::copy).toList());
        for(var source:drops){var stack=source.copy();
            for(int i=0;i<kind().slots&&!stack.isEmpty();i++){var dest=copy.get(i);if(!dest.isEmpty()&&ItemStack.isSameItemSameComponents(dest,stack)){int n=Math.min(stack.getCount(),dest.getMaxStackSize()-dest.getCount());dest.grow(n);stack.shrink(n);}}
            for(int i=0;i<kind().slots&&!stack.isEmpty();i++)if(copy.get(i).isEmpty()){int n=Math.min(stack.getCount(),stack.getMaxStackSize());copy.set(i,stack.split(n));}
            if(!stack.isEmpty())return false;
        }
        if(!simulate){for(int i=0;i<kind().slots;i++)items.set(i,copy.get(i));setChanged();}return true;
    }
    public boolean worldWork(){
        var side=getBlockState().getValue(PowerBlock.FACING);var front=worldPosition.relative(side);if(phantomWork()){if(!boundAvailable())return false;front=bound;if(kind()==WorkKind.PHANTOM_PLACER)side=Direction.UP;}
        if(!level.hasChunkAt(front))return false;
        if(kind()==WorkKind.DROPPER){
            for(var stack:items)if(!stack.isEmpty()){var center=Vec3.atCenterOf(front);var drop=new ItemEntity(level,center.x,center.y,center.z,stack.copyWithCount(1));drop.setDeltaMovement(side.getStepX()*.1,side.getStepY()*.1,side.getStepZ()*.1);drop.setDefaultPickUpDelay();if(level.addFreshEntity(drop)){stack.shrink(1);setChanged();return true;}return false;}return false;
        }
        if(kind()==WorkKind.PLACER||kind()==WorkKind.PHANTOM_PLACER){
            var fake=net.fabricmc.fabric.api.entity.FakePlayer.get((ServerLevel)level);
            for(int i=0;i<items.size();i++)if(!items.get(i).isEmpty()){
                var previous=fake.getMainHandItem();fake.setPos(Vec3.atCenterOf(worldPosition));fake.setItemInHand(InteractionHand.MAIN_HAND,items.get(i).copy());
                try{var result=fake.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(fake,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(front).relative(side,-.5),side,front.relative(side.getOpposite()),false)));
                    if(result.consumesAction()){items.set(i,fake.getMainHandItem().copy());setChanged();return true;}
                }finally{fake.setItemInHand(InteractionHand.MAIN_HAND,previous);}
            }return false;
        }
        boolean active=false;int range=kind()==WorkKind.LONG_BREAKER?8:1;
        if(kind()==WorkKind.LONG_BREAKER&&energy<40)return false;
        for(int i=1;i<=range;i++){
            var p=phantomWork()?front:worldPosition.relative(side,i);if(!level.hasChunkAt(p))break;var state=level.getBlockState(p);
            if(state.isAir()||state.getDestroySpeed(level,p)<0)continue;
            if(harvest(p,state)){if(kind()==WorkKind.LONG_BREAKER)energy-=5;active=true;}
        }if(active)setChanged();return active;
    }
    private boolean harvest(BlockPos pos,BlockState state){
        var server=(ServerLevel)level;var fake=net.fabricmc.fabric.api.entity.FakePlayer.get(server);var entity=level.getBlockEntity(pos);
        if(!net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,fake,pos,state,entity))return false;
        var drops=Block.getDrops(state,server,pos,entity,fake,ItemStack.EMPTY);if(!insertAll(drops,true))return false;
        if(!level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()))return false;
        insertAll(drops,false);state.spawnAfterBreak(server,pos,ItemStack.EMPTY,true);level.levelEvent(2001,pos,Block.getId(state));
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level,fake,pos,state,entity);return true;
    }
    private boolean canFit(int lane,List<ItemStack> rewards){
        ItemStack a=items.get(lane*3+1).copy(),b=items.get(lane*3+2).copy();
        for(var reward:rewards){int remaining=reward.getCount();for(var dest:List.of(a,b))if(!dest.isEmpty()&&ItemStack.isSameItemSameComponents(dest,reward)){int n=Math.min(remaining,dest.getMaxStackSize()-dest.getCount());dest.grow(n);remaining-=n;}
            if(remaining>0&&a.isEmpty()){int n=Math.min(remaining,reward.getMaxStackSize());a=reward.copyWithCount(n);remaining-=n;}
            if(remaining>0&&b.isEmpty()){int n=Math.min(remaining,reward.getMaxStackSize());b=reward.copyWithCount(n);remaining-=n;}
            if(remaining>0)return false;
        }return true;
    }
    private boolean process(int lane){
        if(!pending[lane].isEmpty())return false;
        int slot=lane*3;var input=items.get(slot);
        List<ItemStack> preview;String key;int duration,cost;Processing.Entry crushing=null;
        if(kind()==WorkKind.FURNACE){
            var recipe=((ServerLevel)level).getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(input),level);
            if(recipe.isEmpty()){reset(lane);return false;}
            preview=List.of(recipe.get().value().assemble(new SingleRecipeInput(input)));key=recipe.get().id().toString();duration=80;cost=25;
        }else{
            var recipe=Processing.find(level,"crushing",input);if(recipe.isEmpty()){reset(lane);return false;}
            crushing=recipe.get();preview=Processing.outputs(level,crushing,false);key=crushing.key();duration=kind()==WorkKind.DOUBLE_CRUSHER?150:100;cost=40;
        }
        if(!key.equals(jobKey[lane])||!same(jobInput[lane],input)){progress[lane]=0;jobInput[lane]=input.copyWithCount(1);jobKey[lane]=key;setChanged();}
        if(!canFit(lane,preview)||energy<cost)return false;
        energy-=cost;progress[lane]++;setChanged();if(lane==0&&kind()!=WorkKind.FURNACE&&progress[lane]%20==1)MachineSounds.play(this,"crusher",.025F);
        if(progress[lane]>=duration){
            pending[lane].addAll(crushing==null?preview:Processing.outputs(level,crushing,true));
            input.shrink(1);reset(lane);flush(lane);
        }
        return true;
    }
    private void reset(int lane){if(progress[lane]!=0||!jobKey[lane].isEmpty()){progress[lane]=0;jobInput[lane]=ItemStack.EMPTY;jobKey[lane]="";setChanged();}}
    private static boolean same(ItemStack a,ItemStack b){return a.isEmpty()&&b.isEmpty()||!a.isEmpty()&&!b.isEmpty()&&ItemStack.isSameItemSameComponents(a,b);}
    private void flush(int lane){
        boolean changed=false;
        for(var stack:pending[lane])for(int i=lane*3+1;i<=lane*3+2&&!stack.isEmpty();i++){
            var target=items.get(i);
            if(target.isEmpty()){items.set(i,stack.split(Math.min(stack.getCount(),stack.getMaxStackSize())));changed=true;}
            else if(ItemStack.isSameItemSameComponents(target,stack)){int n=Math.min(stack.getCount(),target.getMaxStackSize()-target.getCount());if(n>0){target.grow(n);stack.shrink(n);changed=true;}}
        }
        pending[lane].removeIf(ItemStack::isEmpty);if(changed)setChanged();
    }
    private boolean empower(){
        if(items.getFirst().getCount()!=1){reset(0);return false;}
        WorkEntity[] stands=new WorkEntity[4];int n=0;
        for(var side:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var p=worldPosition.relative(side,3);if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof WorkEntity stand)||stand.kind()!=WorkKind.DISPLAY){reset(0);return false;}stands[n++]=stand;
        }
        for(var recipe:Processing.all(level,"empowering")){
            if(!Processing.matches(recipe.data().get("base"),items.getFirst()))continue;
            var inputs=Arrays.stream(stands).map(s->s.items.getFirst()).toList();
            if(Processing.assign(recipe.data().getAsJsonArray("modifiers"),inputs)==null)continue;
            var output=Processing.stack(level,recipe.data().get("result"));if(output.isEmpty()||output.getCount()>output.getMaxStackSize())return false;
            if(!recipe.key().equals(jobKey[0])||!same(jobInput[0],items.getFirst())){progress[0]=0;jobKey[0]=recipe.key();jobInput[0]=items.getFirst().copyWithCount(1);}
            // Snapshot each modifier's exact item/components; replacing it starts a fresh job.
            String modifiers=inputs.stream().map(s->s.getItem()+"/"+s.getComponents().hashCode()).toList().toString();
            String signature=recipe.key()+modifiers;
            if(!signature.equals(jobKey[1])){progress[0]=0;jobKey[1]=signature;}
            int time=Math.max(1,recipe.number("time",50)),total=recipe.number("energy",5000);
            int charge=(int)((long)total*(progress[0]+1)/time-(long)total*progress[0]/time);
            for(var stand:stands)if(stand.energy<charge)return false;
            for(var stand:stands){stand.energy-=charge;stand.setChanged();}
            progress[0]++;setChanged();
            if(progress[0]>=time){
                // A display holds one base item, matching the original altar interaction.
                if(items.getFirst().getCount()!=1){reset(0);return false;}
                for(var stand:stands){stand.items.getFirst().shrink(1);stand.setChanged();}
                items.set(0,output);reset(0);jobKey[1]="";
            }
            return true;
        }
        reset(0);jobKey[1]="";return false;
    }
    public boolean spend(int n){if(n<0||energy<n)return false;energy-=n;setChanged();return true;}
    public boolean fire(){
        var lens=items.getFirst().getItem() instanceof Lenses l?l:null;int distance=lens==null?10:lens.distance;
        if(!(level instanceof ServerLevel server)||energy<1000+(lens==null?0:lens.minimum))return false;
        energy-=1000;setChanged();MachineSounds.play(this,"reconstructor",.35F);var side=getBlockState().getValue(PowerBlock.FACING);
        BlockPos end=worldPosition.relative(side,distance);
        for(int d=1;d<=distance;d++){var p=worldPosition.relative(side,d);if(!level.hasChunkAt(p)){end=worldPosition.relative(side,d-1);break;}if(!level.getBlockState(p).isAir()){end=p;break;}}
        if(lens!=null){var start=Vec3.atCenterOf(worldPosition);var finish=Vec3.atCenterOf(end);lens.fire(this,server,end,new AABB(start,finish).inflate(.35));beam(server,start,finish);return true;}
        // Convert the 3x3 impact plane, protecting inventories/block entities from replacement.
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
            BlockPos p=switch(side.getAxis()){case X->end.offset(0,a,b);case Y->end.offset(a,0,b);case Z->end.offset(a,b,0);};
            if(!level.hasChunkAt(p)||level.getBlockEntity(p)!=null)continue;
            var state=level.getBlockState(p);if(state.isAir())continue;
            var recipe=Processing.find(level,"laser",new ItemStack(state.getBlock()));if(recipe.isEmpty())continue;
            int cost=Math.max(1,recipe.get().number("energy",40));if(energy<cost)continue;
            var out=Processing.stack(level,recipe.get().data().get("result"));if(out.isEmpty())continue;
            if(out.getItem() instanceof BlockItem block&&out.getCount()==1)level.setBlock(p,block.getBlock().defaultBlockState(),3);
            else{level.removeBlock(p,false);drop(p.getX()+.5,p.getY()+.5,p.getZ()+.5,out);}
            energy-=cost;setChanged();
        }
        var start=Vec3.atCenterOf(worldPosition);var finish=Vec3.atCenterOf(end);
        var bounds=new AABB(start,finish).inflate(.35);
        for(var entity:server.getEntitiesOfClass(ItemEntity.class,bounds)){
            var input=entity.getItem();var recipe=Processing.find(level,"laser",input);if(recipe.isEmpty())continue;
            int cost=Math.max(1,recipe.get().number("energy",40));int amount=Math.min(input.getCount(),energy/cost);if(amount==0)continue;
            var output=Processing.stack(level,recipe.get().data().get("result"));if(output.isEmpty())continue;
            input.shrink(amount);if(input.isEmpty())entity.discard();else entity.setItem(input.copy());
            int count=output.getCount()*amount;
            while(count>0){int take=Math.min(count,output.getMaxStackSize());drop(entity.getX(),entity.getY(),entity.getZ(),output.copyWithCount(take));count-=take;}
            energy-=cost*amount;setChanged();break;
        }
        beam(server,start,finish);
        return true;
    }
    private void beam(ServerLevel server,Vec3 start,Vec3 finish){var delta=finish.subtract(start);for(int i=1;i<=20;i++){var p=start.add(delta.scale(i/20D));server.sendParticles(new DustParticleOptions(0x1B6FFF,.7F),p.x,p.y,p.z,1,0,0,0,0);}}
    private void drop(double x,double y,double z,ItemStack stack){var entity=new ItemEntity(level,x,y,z,stack);entity.setDefaultPickUpDelay();level.addFreshEntity(entity);}
    public final ContainerData data=new ContainerData(){
        public int getCount(){return 28;}
        public int get(int i){return switch(i){case 0->energy()&65535;case 1->energy()>>>16;case 2->progress[0];case 3->progress[1];case 4->kind().ordinal();case 5->pulseMode?1:0;case 6->level!=null&&level.hasNeighborSignal(worldPosition)?1:0;case 7->capacity()&65535;case 8->capacity()>>>16;case 9->(int)(inputTank.amount/81);case 10->(int)(outputTank.amount/81);case 11->net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(inputTank.variant.getFluid());case 12->net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(outputTank.variant.getFluid());case 13->coffeeCache;case 14->network.priority;case 15->network.mode;default->i>=16&&i<28?lighting.settings[i-16]:0;};}
        public void set(int i,int value){}
    };
    @Override public Component getDisplayName(){return Component.translatable("block.actuallyadditions."+kind().id);}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new WorkMenu(id,inventory,this,data);}
    @Override public int getContainerSize(){return items.size();}
    @Override public int getMaxStackSize(ItemStack stack){return kind()==WorkKind.DISPLAY||kind()==WorkKind.EMPOWERER?1:Math.min(64,stack.getMaxStackSize());}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<items.size()?items.get(slot):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count){var stack=items.get(slot).split(count);setChanged();return stack;}
    @Override public ItemStack removeItemNoUpdate(int slot){var stack=items.get(slot);items.set(slot,ItemStack.EMPTY);return stack;}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        return switch(kind()){case DISPLAY,EMPOWERER->slot==0;case ENERGIZER,ENERVATOR,BATTERY_BOX->slot==0&&stack.getItem() instanceof EnergyItem;case PHANTOM_ITEM,PHANTOM_FLUID,PHANTOM_ENERGY,PHANTOM_REDSTONE,SHOCK,LAMP_CONTROLLER,FIREWORK,ITEM_RELAY,FLUID_RELAY,ITEM_INTERFACE,HOPPING_INTERFACE,PLAYER_INTERFACE,HEAT,LEAF,OIL,FERMENT,LAVA,FLUID_COLLECTOR,FLUID_PLACER->false;case PHANTOM_BREAKER,PHANTOM_PLACER->slot<9;case ADVANCED_ITEM_RELAY->slot<2&&stack.getItem() instanceof FilterItem;case COFFEE->slot==0?stack.is(de.ellpeck.actuallyadditions.fabric.power.PowerMod.ITEMS.get("coffee_beans")):slot==1?stack.is(de.ellpeck.actuallyadditions.fabric.power.PowerMod.ITEMS.get("empty_cup")):slot>=3&&slot<11&&Processing.find(level,"coffee_ingredient",stack).isPresent();case RECONSTRUCTOR->slot==0&&stack.getItem() instanceof Lenses;case DIGGER->slot<9;case COLLECTOR->slot==6&&stack.getItem() instanceof FilterItem;case FARMER->slot<6&&seed(stack);case FEEDER->slot==0;case XP->slot==1&&stack.is(de.ellpeck.actuallyadditions.fabric.power.PowerMod.ITEMS.get("solidified_experience"));case PRESS->slot==0&&Processing.find(level,"pressing",stack).isPresent();case BIO->slot<8&&bioFuel(stack);case BREAKER,PLACER,DROPPER,LONG_BREAKER->slot<9;case FURNACE,DOUBLE_CRUSHER->slot==0||slot==3;case CRUSHER->slot==0;};
    }
    @Override public int[] getSlotsForFace(Direction side){if(kind().slots==0)return new int[0];if(kind()==WorkKind.PRESS||kind()==WorkKind.RECONSTRUCTOR)return new int[]{0};if(kind()==WorkKind.COFFEE)return side==Direction.DOWN?new int[]{2,3,4,5,6,7,8,9,10}:new int[]{0,1,3,4,5,6,7,8,9,10};if(phantomWork())return java.util.stream.IntStream.range(0,9).toArray();if(kind()==WorkKind.DIGGER)return java.util.stream.IntStream.range(0,9).toArray();if(kind()==WorkKind.COLLECTOR)return new int[]{0,1,2,3,4,5};if(kind()==WorkKind.FARMER)return side==Direction.DOWN?new int[]{6,7,8,9,10,11}:new int[]{0,1,2,3,4,5};if(kind()==WorkKind.FEEDER)return new int[]{0};if(kind()==WorkKind.XP)return side==Direction.DOWN?new int[]{0}:new int[]{1};if(kind()==WorkKind.DISPLAY||kind()==WorkKind.EMPOWERER||kind()==WorkKind.BATTERY_BOX)return new int[]{0};if(kind()==WorkKind.ENERGIZER||kind()==WorkKind.ENERVATOR)return side==Direction.DOWN?new int[]{1}:new int[]{0};if(kind()==WorkKind.BIO||kind()==WorkKind.BREAKER||kind()==WorkKind.PLACER||kind()==WorkKind.DROPPER||kind()==WorkKind.LONG_BREAKER)return java.util.stream.IntStream.range(0,kind().slots).toArray();return java.util.Arrays.stream(side==Direction.DOWN?new int[]{1,2,4,5}:new int[]{0,3}).filter(i->i<kind().slots).toArray();}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return kind()!=WorkKind.PHANTOM_BREAKER&&kind()!=WorkKind.DIGGER&&kind()!=WorkKind.BREAKER&&kind()!=WorkKind.LONG_BREAKER&&side!=Direction.DOWN&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){if(kind()==WorkKind.COFFEE)return slot==2||slot>=3&&Processing.find(level,"coffee_ingredient",stack).isEmpty();if(phantomWork()||kind()==WorkKind.DIGGER)return true;if(kind()==WorkKind.COLLECTOR)return slot<6;if(kind()==WorkKind.FARMER)return slot>=6&&slot<12;if(kind()==WorkKind.BIO||kind()==WorkKind.FEEDER)return false;if(kind()==WorkKind.XP)return slot==0;if(kind()==WorkKind.BREAKER||kind()==WorkKind.PLACER||kind()==WorkKind.DROPPER||kind()==WorkKind.LONG_BREAKER)return true;return side==Direction.DOWN&&((kind()==WorkKind.DISPLAY||kind()==WorkKind.EMPOWERER||kind()==WorkKind.BATTERY_BOX)?slot==0:(slot==1||slot==2||slot==4||slot==5));}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void setChanged(){super.setChanged();if(level instanceof ServerLevel&&(kind()==WorkKind.DISPLAY||kind()==WorkKind.EMPOWERER||kind()==WorkKind.BATTERY_BOX))level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level instanceof ServerLevel){if(isTransportRelay()){network.unlink();if(network.rangeUpgrade)Block.popResource(level,pos,new ItemStack(de.ellpeck.actuallyadditions.fabric.power.PowerMod.ITEMS.get("laser_upgrade_range")));if(network.invisible)Block.popResource(level,pos,new ItemStack(de.ellpeck.actuallyadditions.fabric.power.PowerMod.ITEMS.get("laser_upgrade_invisibility")));}for(var stack:items)if(!stack.isEmpty())Block.popResource(level,pos,stack.copy());for(var queue:pending)for(var stack:queue)if(!stack.isEmpty())Block.popResource(level,pos,stack.copy());}items.clear();for(var queue:pending)queue.clear();super.preRemoveSideEffects(pos,state);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);if(kind()==WorkKind.FIREWORK)lighting.save(out.child("lighting"));if(isTransportRelay())network.save(out.child("network"));if(playerId!=null)out.putString("linked_player",playerId.toString());out.putInt("energy",energy);inputTank.save(out.child("tank_input"));outputTank.save(out.child("tank_output"));out.putInt("counter",counter);out.putInt("burn",burn);out.putInt("production",production);out.putInt("heatAge",heatAge);out.putLong("xp_points",experience);out.putInt("farm_index",farmIndex);out.putInt("coffee_cache",coffeeCache);out.putBoolean("brewing",brewing);out.putInt("dig_index",digIndex);out.putInt("dig_y",digY);out.putBoolean("only_ores",onlyOres);if(bound!=null)out.store("bound",BlockPos.CODEC,bound);out.putBoolean("pulse",pulseMode);out.putBoolean("signal",lastSignal);
        for(int i=0;i<12;i++)if(!items.get(i).isEmpty())out.store("slot_"+i,ItemStack.CODEC,items.get(i));
        for(int lane=0;lane<2;lane++){out.putInt("progress_"+lane,progress[lane]);out.putString("job_"+lane,jobKey[lane]);if(!jobInput[lane].isEmpty())out.store("input_"+lane,ItemStack.CODEC,jobInput[lane]);out.store("pending_"+lane,ItemStack.CODEC.listOf(),pending[lane]);}
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);in.child("lighting").ifPresent(lighting::load);in.child("network").ifPresent(network::load);try{playerId=java.util.UUID.fromString(in.getStringOr("linked_player",""));}catch(IllegalArgumentException ignored){playerId=null;}coffeeCache=Math.clamp(in.getIntOr("coffee_cache",0),0,300);brewing=in.getBooleanOr("brewing",false);bound=in.read("bound",BlockPos.CODEC).orElse(null);if(level!=null&&!in.getStringOr("bound_dimension",level.dimension().identifier().toString()).equals(level.dimension().identifier().toString()))bound=null;digIndex=Math.max(0,in.getIntOr("dig_index",0));digY=in.getIntOr("dig_y",Integer.MIN_VALUE);onlyOres=in.getBooleanOr("only_ores",false);farmIndex=Math.floorMod(in.getIntOr("farm_index",0),PortConfig.area("farmerArea")*PortConfig.area("farmerArea"));experience=Math.clamp(in.getLongOr("xp_points",0),0,Integer.MAX_VALUE);in.child("tank_input").ifPresent(inputTank::load);in.child("tank_output").ifPresent(outputTank::load);energy=Math.clamp(in.getIntOr("energy",0),0,capacity());counter=Math.clamp(in.getIntOr("counter",0),0,100);burn=Math.clamp(in.getIntOr("burn",0),0,120000);production=Math.clamp(in.getIntOr("production",0),0,256);heatAge=Math.clamp(in.getIntOr("heatAge",0),0,1000);pulseMode=in.getBooleanOr("pulse",false);lastSignal=in.getBooleanOr("signal",false);
        for(int i=0;i<12;i++)items.set(i,in.read("slot_"+i,ItemStack.CODEC).orElse(ItemStack.EMPTY));
        for(int lane=0;lane<2;lane++){progress[lane]=Math.clamp(in.getIntOr("progress_"+lane,0),0,120000);jobKey[lane]=in.getStringOr("job_"+lane,"");jobInput[lane]=in.read("input_"+lane,ItemStack.CODEC).orElse(ItemStack.EMPTY);pending[lane].clear();pending[lane].addAll(in.read("pending_"+lane,ItemStack.CODEC.listOf()).orElse(List.of()));}
    }
}
