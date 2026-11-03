package de.ellpeck.actuallyadditions.fabric.content;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.level.storage.*;
/** A transactional tank with separately controlled external input and output. */
public final class MachineTank extends SingleVariantStorage<FluidVariant> {
    private final WorkEntity owner;private final boolean input;private boolean internal;
    public MachineTank(WorkEntity owner,boolean input){this.owner=owner;this.input=input;}
    @Override protected FluidVariant getBlankVariant(){return FluidVariant.blank();}
    @Override protected long getCapacity(FluidVariant resource){return 2000L*81;}
    @Override protected boolean canInsert(FluidVariant resource){return internal||input&&owner.acceptsFluid(resource);}
    @Override protected boolean canExtract(FluidVariant resource){return internal||!input;}
    @Override protected void onFinalCommit(){owner.setChanged();}
    public long add(FluidVariant fluid,long amount,TransactionContext transaction){internal=true;try{return super.insert(fluid,amount,transaction);}finally{internal=false;}}
    public long take(FluidVariant fluid,long amount,TransactionContext transaction){internal=true;try{return super.extract(fluid,amount,transaction);}finally{internal=false;}}
    public void save(ValueOutput out){SingleVariantStorage.writeValue(this,FluidVariant.CODEC,out);}
    public void load(ValueInput in){SingleVariantStorage.readValue(this,FluidVariant.CODEC,FluidVariant::blank,in);amount=Math.clamp(amount,0,getCapacity());}
}
