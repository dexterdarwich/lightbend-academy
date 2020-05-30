package com.lightbend.futures;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

interface CustomerRepository {
    void saveCustomer(Customer customer);
    Optional<Customer> getCustomer(UUID customerId);
}

class CachedCustomerRepository implements CustomerRepository {

    private ObjectStore objectStore;
    private ConcurrentHashMap<UUID, Customer> cache = new ConcurrentHashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    CachedCustomerRepository(ObjectStore objectStore) {
        this.objectStore = objectStore;
    }

    @Override
    public void saveCustomer(Customer customer) {
        lock.writeLock().lock();

        objectStore.write(customer.getId(), customer);
        cache.put(customer.getId(), customer);

        lock.writeLock().unlock();
    }

    @Override
    public Optional<Customer> getCustomer(UUID customerId) {
        lock.readLock().lock();

        Optional<Customer> result;

        if(cache.containsKey(customerId)) {
            result = Optional.of(cache.get(customerId));
        } else {
            result = objectStore.read(customerId).map(obj -> (Customer) obj);
        }

        lock.readLock().unlock();

        return result;
    }
}
