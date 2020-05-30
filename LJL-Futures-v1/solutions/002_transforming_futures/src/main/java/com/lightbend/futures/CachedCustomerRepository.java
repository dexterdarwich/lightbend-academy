package com.lightbend.futures;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

interface CustomerRepository {
    CompletableFuture<Void> saveCustomer(Customer customer);
    CompletableFuture<Optional<Customer>> getCustomer(UUID customerId);
}

class CachedCustomerRepository implements CustomerRepository {

    private ObjectStore objectStore;
    private ConcurrentHashMap<UUID, Customer> cache = new ConcurrentHashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    CachedCustomerRepository(ObjectStore objectStore) {
        this.objectStore = objectStore;
    }

    @Override
    public CompletableFuture<Void> saveCustomer(Customer customer) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();

            objectStore.write(customer.getId(), customer);
            cache.put(customer.getId(), customer);

            lock.writeLock().unlock();
        });
    }

    @Override
    public CompletableFuture<Optional<Customer>> getCustomer(UUID customerId) {
        lock.readLock().lock();

        CompletableFuture<Optional<Customer>> result;

        if(cache.containsKey(customerId)) {
            result = CompletableFuture.completedFuture(
                Optional.of(cache.get(customerId))
            );
        } else {
            result = CompletableFuture.supplyAsync(() ->
                objectStore.read(customerId).map(obj -> (Customer) obj)
            );
        }

        lock.readLock().unlock();

        return result;
    }
}
