package de.ellpeck.actuallyadditions.fabric.content;
import java.util.*;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
/** Resolves the target afresh on every operation, so cached lookups cannot access unloaded inventories. */
public final class RemoteStorage<T> implements Storage<T> {
    private final Supplier<Storage<T>> target;
    public RemoteStorage(Supplier<Storage<T>> target){this.target=target;}
    @Override public long insert(T resource,long amount,TransactionContext tx){var s=target.get();return s==null?0:s.insert(resource,amount,tx);}
    @Override public long extract(T resource,long amount,TransactionContext tx){var s=target.get();return s==null?0:s.extract(resource,amount,tx);}
    @Override public Iterator<StorageView<T>> iterator(){var s=target.get();return s==null?Collections.emptyIterator():s.iterator();}
    @Override public boolean supportsInsertion(){var s=target.get();return s!=null&&s.supportsInsertion();}
    @Override public boolean supportsExtraction(){var s=target.get();return s!=null&&s.supportsExtraction();}
}
