package com.lightbend.futures;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class CustomerService {

    private CustomerRepository customerRepository;

    CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    CompletableFuture<UUID> addCustomer(String firstName, String lastName, String address, String phoneNumber) {
        UUID customerId = UUID.randomUUID();
        return customerRepository.saveCustomer(new Customer(
            customerId,
            firstName,
            lastName,
            address,
            phoneNumber
        )).thenApply(ignored -> customerId);
    }

    CompletableFuture<Optional<String>> getCustomerFirstName(UUID customerId) {
        return customerRepository.getCustomer(customerId)
                .thenApply(opt -> opt.map(Customer::getFirstName));
    }

    CompletableFuture<Optional<String>> getCustomerLastName(UUID customerId) {
        return customerRepository.getCustomer(customerId)
                .thenApply(opt -> opt.map(Customer::getLastName));
    }

    CompletableFuture<Optional<String>> getCustomerAddress(UUID customerId) {
        return customerRepository.getCustomer(customerId)
                .thenApply(opt -> opt.map(Customer::getAddress));
    }

    CompletableFuture<Optional<String>> getCustomerPhoneNumber(UUID customerId) {
        return customerRepository.getCustomer(customerId)
                .thenApply(opt -> opt.map(Customer::getPhoneNumber));
    }
}
