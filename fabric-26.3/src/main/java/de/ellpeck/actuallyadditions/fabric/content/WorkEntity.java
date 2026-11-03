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
    private final NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY);
    private final List<ItemStack>[] pending=new List[]{new ArrayList<>(),new ArrayList<>()};
    private final ItemStack[] jobInput={ItemStack.EMPTY,ItemStack.EMPTY};
    private final String[] jobKey={"",""};
    private final int[] progress={0,0};
    private int energy,counter,burn,production,heatAge;
    public final MachineTank inputTank=new MachineTank(this,true), outputTank=new MachineTank(this,false);
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> fluidStorage(){
        return switch(kind()){case PRESS->outputTank;case OIL->inputTank;case FERMENT->new net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage<>(List.of(inputTank,outputTank));default->null;};
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
            case DISPLAY -> {}
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
    private static net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid(JsonObject definition){return net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(net.minecraft.resources.Identifier.parse(definition.get("id").getAsString())));}
    private static long volume(JsonObject definition){return Math.max(0,definition.get("amount").getAsLong())*81;}
    public boolean acceptsFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid){
        if(level==null)return false;
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
    private int outputRate(){return switch(kind()){case HEAT->80;case LEAF->450;case BIO->800;case OIL->500;default->1000;};}
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
        }else if(enabled&&kind()==WorkKind.LEAF&&++counter>=5){
            counter=0;
            if(energy+300<=capacity()){
                List<BlockPos> leaves=new ArrayList<>();
                for(var p:BlockPos.betweenClosed(worldPosition.offset(-7,-7,-7),worldPosition.offset(7,7,7)))if(level.hasChunkAt(p)&&level.getBlockState(p).is(net.minecraft.tags.BlockTags.LEAVES))leaves.add(p.immutable());
                if(!leaves.isEmpty()){var p=leaves.get(level.getRandom().nextInt(leaves.size()));level.levelEvent(2001,p,Block.getId(level.getBlockState(p)));level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());energy+=300;active=true;}
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
        var side=getBlockState().getValue(PowerBlock.FACING);var front=worldPosition.relative(side);
        if(!level.hasChunkAt(front))return false;
        if(kind()==WorkKind.DROPPER){
            for(var stack:items)if(!stack.isEmpty()){var center=Vec3.atCenterOf(front);var drop=new ItemEntity(level,center.x,center.y,center.z,stack.copyWithCount(1));drop.setDeltaMovement(side.getStepX()*.1,side.getStepY()*.1,side.getStepZ()*.1);drop.setDefaultPickUpDelay();if(level.addFreshEntity(drop)){stack.shrink(1);setChanged();return true;}return false;}return false;
        }
        if(kind()==WorkKind.PLACER){
            var fake=net.fabricmc.fabric.api.entity.FakePlayer.get((ServerLevel)level);
            for(int i=0;i<items.size();i++)if(!items.get(i).isEmpty()){
                var previous=fake.getMainHandItem();fake.setPos(Vec3.atCenterOf(worldPosition));fake.setItemInHand(InteractionHand.MAIN_HAND,items.get(i).copy());
                try{var result=fake.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(fake,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(worldPosition).relative(side,.5),side,worldPosition,false)));
                    if(result.consumesAction()){items.set(i,fake.getMainHandItem().copy());setChanged();return true;}
                }finally{fake.setItemInHand(InteractionHand.MAIN_HAND,previous);}
            }return false;
        }
        boolean active=false;int range=kind()==WorkKind.LONG_BREAKER?8:1;
        if(kind()==WorkKind.LONG_BREAKER&&energy<40)return false;
        for(int i=1;i<=range;i++){
            var p=worldPosition.relative(side,i);if(!level.hasChunkAt(p))break;var state=level.getBlockState(p);
            if(state.isAir()||state.getDestroySpeed(level,p)<0||level.getBlockEntity(p)!=null)continue;
            var fake=net.fabricmc.fabric.api.entity.FakePlayer.get((ServerLevel)level);
            if(!net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,fake,p,state,null))continue;
            var drops=Block.getDrops(state,(ServerLevel)level,p,null);
            if(!insertAll(drops,true))continue;
            if(level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState())){insertAll(drops,false);level.levelEvent(2001,p,Block.getId(state));if(kind()==WorkKind.LONG_BREAKER)energy-=5;active=true;}
        }if(active)setChanged();return active;
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
        energy-=cost;progress[lane]++;setChanged();
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
    public boolean fire(){
        if(!(level instanceof ServerLevel server)||energy<1000)return false;
        energy-=1000;setChanged();var side=getBlockState().getValue(PowerBlock.FACING);
        BlockPos end=worldPosition.relative(side,10);
        for(int d=1;d<=10;d++){var p=worldPosition.relative(side,d);if(!level.hasChunkAt(p)){end=worldPosition.relative(side,d-1);break;}if(!level.getBlockState(p).isAir()){end=p;break;}}
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
        var delta=finish.subtract(start);for(int i=1;i<=20;i++){var p=start.add(delta.scale(i/20D));server.sendParticles(new DustParticleOptions(0x1B6FFF,.7F),p.x,p.y,p.z,1,0,0,0,0);}
        return true;
    }
    private void drop(double x,double y,double z,ItemStack stack){var entity=new ItemEntity(level,x,y,z,stack);entity.setDefaultPickUpDelay();level.addFreshEntity(entity);}
    public final ContainerData data=new ContainerData(){
        public int getCount(){return 9;}
        public int get(int i){return switch(i){case 0->energy()&65535;case 1->energy()>>>16;case 2->progress[0];case 3->progress[1];case 4->kind().ordinal();case 5->pulseMode?1:0;case 6->level!=null&&level.hasNeighborSignal(worldPosition)?1:0;case 7->capacity()&65535;case 8->capacity()>>>16;default->0;};}
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
        return switch(kind()){case DISPLAY,EMPOWERER->slot==0;case ENERGIZER,ENERVATOR,BATTERY_BOX->slot==0&&stack.getItem() instanceof EnergyItem;case RECONSTRUCTOR,HEAT,LEAF,OIL,FERMENT->false;case PRESS->slot==0&&Processing.find(level,"pressing",stack).isPresent();case BIO->slot<8&&bioFuel(stack);case BREAKER,PLACER,DROPPER,LONG_BREAKER->slot<9;case FURNACE,DOUBLE_CRUSHER->slot==0||slot==3;case CRUSHER->slot==0;};
    }
    @Override public int[] getSlotsForFace(Direction side){if(kind()==WorkKind.DISPLAY||kind()==WorkKind.EMPOWERER||kind()==WorkKind.BATTERY_BOX)return new int[]{0};if(kind()==WorkKind.ENERGIZER||kind()==WorkKind.ENERVATOR)return side==Direction.DOWN?new int[]{1}:new int[]{0};if(kind()==WorkKind.BIO||kind()==WorkKind.BREAKER||kind()==WorkKind.PLACER||kind()==WorkKind.DROPPER||kind()==WorkKind.LONG_BREAKER)return java.util.stream.IntStream.range(0,kind().slots).toArray();return side==Direction.DOWN?new int[]{1,2,4,5}:new int[]{0,3};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return kind()!=WorkKind.BREAKER&&kind()!=WorkKind.LONG_BREAKER&&side!=Direction.DOWN&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){if(kind()==WorkKind.BIO)return false;if(kind()==WorkKind.BREAKER||kind()==WorkKind.PLACER||kind()==WorkKind.DROPPER||kind()==WorkKind.LONG_BREAKER)return true;return side==Direction.DOWN&&((kind()==WorkKind.DISPLAY||kind()==WorkKind.EMPOWERER||kind()==WorkKind.BATTERY_BOX)?slot==0:(slot==1||slot==2||slot==4||slot==5));}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level instanceof ServerLevel){for(var stack:items)if(!stack.isEmpty())Block.popResource(level,pos,stack.copy());for(var queue:pending)for(var stack:queue)if(!stack.isEmpty())Block.popResource(level,pos,stack.copy());}items.clear();for(var queue:pending)queue.clear();super.preRemoveSideEffects(pos,state);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("energy",energy);inputTank.save(out.child("tank_input"));outputTank.save(out.child("tank_output"));out.putInt("counter",counter);out.putInt("burn",burn);out.putInt("production",production);out.putInt("heatAge",heatAge);out.putBoolean("pulse",pulseMode);out.putBoolean("signal",lastSignal);
        for(int i=0;i<9;i++)if(!items.get(i).isEmpty())out.store("slot_"+i,ItemStack.CODEC,items.get(i));
        for(int lane=0;lane<2;lane++){out.putInt("progress_"+lane,progress[lane]);out.putString("job_"+lane,jobKey[lane]);if(!jobInput[lane].isEmpty())out.store("input_"+lane,ItemStack.CODEC,jobInput[lane]);out.store("pending_"+lane,ItemStack.CODEC.listOf(),pending[lane]);}
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);in.child("tank_input").ifPresent(inputTank::load);in.child("tank_output").ifPresent(outputTank::load);energy=Math.clamp(in.getIntOr("energy",0),0,capacity());counter=Math.clamp(in.getIntOr("counter",0),0,100);burn=Math.clamp(in.getIntOr("burn",0),0,120000);production=Math.clamp(in.getIntOr("production",0),0,256);heatAge=Math.clamp(in.getIntOr("heatAge",0),0,1000);pulseMode=in.getBooleanOr("pulse",false);lastSignal=in.getBooleanOr("signal",false);
        for(int i=0;i<9;i++)items.set(i,in.read("slot_"+i,ItemStack.CODEC).orElse(ItemStack.EMPTY));
        for(int lane=0;lane<2;lane++){progress[lane]=Math.clamp(in.getIntOr("progress_"+lane,0),0,120000);jobKey[lane]=in.getStringOr("job_"+lane,"");jobInput[lane]=in.read("input_"+lane,ItemStack.CODEC).orElse(ItemStack.EMPTY);pending[lane].clear();pending[lane].addAll(in.read("pending_"+lane,ItemStack.CODEC.listOf()).orElse(List.of()));}
    }
}
