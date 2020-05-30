package com.lightbend.futures;

import java.util.Optional;
import java.util.UUID;

class CustomerService {

    private CustomerRepository customerRepository;

    CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    UUID addCustomer(String firstName, String lastName, String address, String phoneNumber) {
        UUID customerId = UUID.randomUUID();
        customerRepository.saveCustomer(new Customer(
            customerId,
            firstName,
            lastName,
            address,
            phoneNumber
        ));

        return customerId;
    }

    Optional<String> getCustomerFirstName(UUID customerId) {
        return customerRepository.getCustomer(customerId).map(Customer::getFirstName);
    }

    Optional<String> getCustomerLastName(UUID customerId) {
        return customerRepository.getCustomer(customerId).map(Customer::getLastName);
    }

    Optional<String> getCustomerAddress(UUID customerId) {
        return customerRepository.getCustomer(customerId).map(Customer::getAddress);
    }

    Optional<String> getCustomerPhoneNumber(UUID customerId) {
        return customerRepository.getCustomer(customerId).map(Customer::getPhoneNumber);
    }
}
