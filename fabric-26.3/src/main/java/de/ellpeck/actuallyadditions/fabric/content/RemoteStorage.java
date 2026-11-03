package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
/** Resolves the target afresh on every operation, so cached lookups cannot access unloaded inventories. */
public final class RemoteStorage<T> implements Storage<T> {
    private final Supplier<Storage<T>> target;
    private final Supplier<?> identity;
    public RemoteStorage(Supplier<Storage<T>> target){this(target,target);}
    public RemoteStorage(Supplier<Storage<T>> target,Supplier<?> identity){this.target=target;this.identity=identity;}
    @Override public long insert(T resource,long amount,TransactionContext tx){var s=target.get();return s==null?0:s.insert(resource,amount,tx);}
    @Override public long extract(T resource,long amount,TransactionContext tx){var s=target.get();return s==null?0:s.extract(resource,amount,tx);}
    @Override public Iterator<StorageView<T>> iterator(){var s=target.get();if(s==null)return Collections.emptyIterator();var token=identity.get();var views=s.iterator();
        return new Iterator<>(){
            public boolean hasNext(){return views.hasNext();}
            public StorageView<T> next(){var view=views.next();return new StorageView<>(){
                private boolean current(){return token!=null&&identity.get()==token&&target.get()!=null;}
                public boolean isResourceBlank(){return !current()||view.isResourceBlank();}
                public T getResource(){return view.getResource();}
                public long getAmount(){return current()?view.getAmount():0;}
                public long getCapacity(){return current()?view.getCapacity():0;}
                public long extract(T resource,long amount,TransactionContext tx){return current()?view.extract(resource,amount,tx):0;}
            };}
        };}
    @Override public boolean supportsInsertion(){var s=target.get();return s!=null&&s.supportsInsertion();}
    @Override public boolean supportsExtraction(){var s=target.get();return s!=null&&s.supportsExtraction();}
}
